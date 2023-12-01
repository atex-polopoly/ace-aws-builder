package com.atex.ace;

import java.util.Arrays;
import java.util.List;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.iam.CfnAccessKey;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.route53.CnameRecord;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.constructs.Construct;
import software.amazon.awscdk.StackProps;

import static software.amazon.awscdk.services.iam.Effect.*;
import static software.amazon.awscdk.services.s3.BlockPublicAccess.*;
import static software.amazon.awscdk.services.s3.BucketEncryption.*;

/**
 * Stack that will construct all basic AWS resources necessary for an ACE installation.
 * This will include:
 *
 * - S3 bucket
 * - IAM user
 * - ACE access policy
 * - Certificates
 * - DNS entries
 */
public class AtexCloudACEBaseStack
    extends AtexCloudAbstractStack
{
    public AtexCloudACEBaseStack(final Construct scope,
                                 final String id,
                                 final StackProps props,
                                 final CommonProperties properties)
    {
        super(scope, id, props, properties);

        // S3 bucket

        Bucket contentFilesBucket = contentFilesBucket();

        asOutput("ContentFilesBucketOutput", contentFilesBucket.getBucketName());

        // TODO: Lookup RDS cluster. Doesn't seem you can do it only based on for example ARN (need to supply all properties)?

        // IAM S3 user (until we no longer need it)

        ManagedPolicy aceContentFilesAccessPolicy =
            ManagedPolicy.Builder.create(this, "ContentFilesBucketAccessPolicy")
                                 .managedPolicyName(String.format("%s-s3-access-policy", properties.customerName()))
                                 .document(PolicyDocument.Builder.create()
                                                                 .statements(List.of(allow("*", "s3:ListAllMyBuckets"),
                                                                                     allow(String.format("arn:aws:s3:::%s/*", contentFilesBucket.getBucketName()), "s3:PutObject", "s3:GetObject")))
                                                                 .build())
                                 .build();

        User contentFilesBucketUser = user(List.of(aceContentFilesAccessPolicy));

        CfnAccessKey contentFilesBucketUserAccessKey = CfnAccessKey.Builder.create(this, "ContentFilesBucketAccessKey")
                                                                           .userName(contentFilesBucketUser.getUserName())
                                                                           .build();

        asOutput("ContentFilesBucketAccessKeyOutput", contentFilesBucketUserAccessKey.getRef());
        asOutput("ContentFilesBucketSecretKeyOutput", contentFilesBucketUserAccessKey.getAttrSecretAccessKey());

        IHostedZone hostedZone = lookupHostedZone();

        // Certificates

        certificate("APICertificate", "api.customer.dev.atexcloud.io", hostedZone);
        certificate("SitemapCertificate", "sitemap.customer.dev.atexcloud.io", hostedZone);
        certificate("WebsiteCertificate", "customer.dev.atexcloud.io", hostedZone);

        // DNS entries

        CnameRecord.Builder.create(this, "sitemap.customer.dev.atexcloud.io")
                           .recordName("sitemap.customer.dev.atexcloud.io")
                           .domainName("atex-Route-1VIM60JZT7VDC-2038118370.eu-west-1.elb.amazonaws.com") // this is dev...
                           .ttl(Duration.minutes(5))
                           .zone(hostedZone)
                           .build();

        // IAM ACE access policy

        ManagedPolicy aceAccessManagedPolicy = ManagedPolicy.Builder.create(this, "ACEAccessPolicy")
                                                                    .managedPolicyName(String.format("%s-ace-access", properties.customerName()))
                                                                    .statements(List.of(allow(String.format("%s/%s-staging", properties.databaseARN(), properties.customerName()), "rds-db:connect"),
                                                                                        allow("arn:aws:events:eu-west-1:103826127765:event-bus/cms-events-staging", "events:PutEvents")))
                                                                    .build();

        asOutput("ACEAccessPolicyOutput", aceAccessManagedPolicy.getManagedPolicyArn());
    }

    private PolicyStatement allow(final String resource,
                                  final String... actions)
    {
        return PolicyStatement.Builder.create()
                                      .effect(ALLOW)
                                      .actions(Arrays.asList(actions))
                                      .resources(List.of(resource))
                                      .build();
    }

    private Certificate certificate(final String certificateName,
                                    final String domainName,
                                    final IHostedZone hostedZone)
    {
        return Certificate.Builder.create(this, certificateName)
                                  .domainName(domainName)
                                  .certificateName(certificateName)
                                  .validation(CertificateValidation.fromDns(hostedZone))
                                  .build();
    }

    private User user(final List<ManagedPolicy> managedPolicies)
    {
        return User.Builder.create(this, "ContentFilesBucketUser")
                           .userName(String.format("%s-s3", properties.customerName()))
                           .managedPolicies(managedPolicies)
                           .build();
    }

    private Bucket contentFilesBucket()
    {
        String bucketName = String.format("atex-cloud.%s-staging.files",
                                          properties.customerName());

        return Bucket.Builder.create(this, "ContentFilesBucket")
                             .bucketName(bucketName)
                             .versioned(false)
                             .encryption(S3_MANAGED)
                             .blockPublicAccess(BLOCK_ALL)
                             .lifecycleRules(List.of(LifecycleRule.builder()
                                                                  .id(String.format("%s-tmp-lifecycle", bucketName)) // TODO: is this an inline policy? in that case no need to include bucket name in it..
                                                                  .expiration(Duration.days(30))
                                                                  .prefix("/tmp")
                                                                  .enabled(true)
                                                                  .abortIncompleteMultipartUploadAfter(Duration.days(30))
                                                                  .build()))
                             .build();
    }
}
