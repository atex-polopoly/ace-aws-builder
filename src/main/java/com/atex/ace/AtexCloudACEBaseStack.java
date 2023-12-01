package com.atex.ace;

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
import software.constructs.Construct;
import software.amazon.awscdk.StackProps;

import static software.amazon.awscdk.services.iam.Effect.*;

public class AtexCloudACEBaseStack
    extends AtexCloudAbstractStack
{
    private final CommonProperties properties;

    public AtexCloudACEBaseStack(final Construct scope,
                                 final String id,
                                 final StackProps props,
                                 final CommonProperties properties)
    {
        super(scope, id, props);

        this.properties = properties;

        // S3 bucket

        Bucket contentFilesBucket = contentFilesBucket();

        asOutput("AceContentFilesBucketName", contentFilesBucket.getBucketName());

        // TODO: Lookup RDS cluster. Doesn't seem you can do it only based on for example ARN (need to supply all properties)?

        // IAM S3 user (until we no longer need it)

        ManagedPolicy aceContentFilesAccessPolicy =
            ManagedPolicy.Builder.create(this, "AceContentFilesBucketAcessPolicy")
                                 .managedPolicyName(String.format("%s-s3-access-policy", properties.customerName()))
                                 .document(PolicyDocument.Builder.create()
                                                                 .statements(List.of(PolicyStatement.Builder.create()
                                                                                                            .effect(ALLOW)
                                                                                                            .actions(List.of("s3:ListAllMyBuckets"))
                                                                                                            .resources(List.of("*"))
                                                                                                            .build(),
                                                                                     PolicyStatement.Builder.create()
                                                                                                            .effect(ALLOW)
                                                                                                            .actions(List.of("s3:PutObject", "s3:GetObject"))
                                                                                                            .resources(List.of(String.format("arn:aws:s3:::%s/*", contentFilesBucket.getBucketName())))
                                                                                                            .build()))
                                                                 .build())
                                 .build();

        User contentFilesBucketUser = user(List.of(aceContentFilesAccessPolicy));

        CfnAccessKey contentFilesBucketUserAccessKey = CfnAccessKey.Builder.create(this, "AceContentFilesBucketAccessKey")
                                                                           .userName(contentFilesBucketUser.getUserName())
                                                                           .build();

        asOutput("AceContentFilesBucketAccessKeyValue", contentFilesBucketUserAccessKey.getRef());
        asOutput("AceContentFilesBucketSecretKeyValue", contentFilesBucketUserAccessKey.getAttrSecretAccessKey());

        IHostedZone hostedZone = lookupHostedZone();

        // Certificates

        certificate("api-customer-cert", "api.customer.dev.atexcloud.io", hostedZone);
        certificate("sitemap-customer-cert", "sitemap.customer.dev.atexcloud.io", hostedZone);
        certificate("website-customer-cert", "customer.dev.atexcloud.io", hostedZone);

        // DNS entries

        CnameRecord.Builder.create(this, "sitemap.customer.dev.atexcloud.io")
                           .recordName("sitemap.customer.dev.atexcloud.io")
                           .domainName("atex-Route-1VIM60JZT7VDC-2038118370.eu-west-1.elb.amazonaws.com") // this is dev...
                           .ttl(Duration.minutes(5))
                           .zone(hostedZone)
                           .build();

        // IAM ACE access policy

        ManagedPolicy aceAccessManagedPolicy = ManagedPolicy.Builder.create(this, "AceAccessPolicy")
                                                                    .managedPolicyName(String.format("%s-ace-access", properties.customerName()))
                                                                    .statements(List.of(PolicyStatement.Builder.create()
                                                                                                               .effect(ALLOW)
                                                                                                               .actions(List.of("rds-db:connect"))
                                                                                                               .resources(List.of(String.format("%s/%s-staging", properties.databaseARN(), properties.customerName())))
                                                                                                               .build(),
                                                                                        PolicyStatement.Builder.create()
                                                                                                               .effect(ALLOW)
                                                                                                               .actions(List.of("events:PutEvents"))
                                                                                                               .resources(List.of("arn:aws:events:eu-west-1:103826127765:event-bus/cms-events-staging"))
                                                                                                               .build()))
                                                                    .build();

        asOutput("AceAccessPolicyARN", aceAccessManagedPolicy.getManagedPolicyArn());
    }

    private Certificate certificate(final String name,
                                    final String domainName,
                                    final IHostedZone hostedZone)
    {
        return Certificate.Builder.create(this, name)
                                  .domainName(domainName)
                                  .certificateName(name)
                                  .validation(CertificateValidation.fromDns(hostedZone))
                                  .build();
    }

    private User user(final List<ManagedPolicy> managedPolicies)
    {
        return User.Builder.create(this, "AceContentFilesBucketUser")
                           .userName(String.format("%s-s3", properties.customerName()))
                           .managedPolicies(managedPolicies)
                           .build();
    }

    private Bucket contentFilesBucket()
    {
        return Bucket.Builder.create(this, "AceContentFilesBucket")
                             .bucketName(String.format("atex-cloud.%s-staging.files", properties.customerName()))
                             .versioned(false)
                             .build();
    }
}
