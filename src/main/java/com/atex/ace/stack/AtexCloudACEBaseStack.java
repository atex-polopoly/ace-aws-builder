package com.atex.ace.stack;

import com.atex.ace.CommonProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.iam.CfnAccessKey;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.constructs.Construct;
import software.amazon.awscdk.StackProps;

import static com.atex.ace.EnvironmentType.*;
import static software.amazon.awscdk.RemovalPolicy.*;
import static software.amazon.awscdk.services.iam.Effect.*;
import static software.amazon.awscdk.services.s3.BlockPublicAccess.*;
import static software.amazon.awscdk.services.s3.BucketEncryption.*;

/**
 * Stack that will construct all basic AWS resources
 * necessary for an ACE installation.
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

        // IAM S3 user (with necessary policy and keys)

        ManagedPolicy contentFilesBucketAccessPolicy = contentFilesBucketAccessPolicy(contentFilesBucket);
        User contentFilesBucketUser = contentFilesBucketUser(List.of(contentFilesBucketAccessPolicy));

        CfnAccessKey contentFilesBucketUserAccessKey = accessKey("ContentFilesBucketAccessKey", contentFilesBucketUser);

        asOutput("ContentFilesBucketAccessKeyOutput", contentFilesBucketUserAccessKey.getRef());
        asOutput("ContentFilesBucketSecretKeyOutput", contentFilesBucketUserAccessKey.getAttrSecretAccessKey());

        // Certificates

        certificate("APICertificate", apiDomainName(), hostedZone);
        certificate("SitemapCertificate", sitemapDomainName(), hostedZone);
//        certificate("WebsiteCertificate", websiteDomainName(), hostedZone);

        // DNS entries

        dnsEntry(sitemapDomainName(), properties.loadBalancerDomain(), hostedZone);

        // ACE access policy

        ManagedPolicy aceAccessPolicy = aceAccessPolicy(contentFilesBucket);
        asOutput("ACEAccessPolicyOutput", aceAccessPolicy.getManagedPolicyArn());
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

    private User contentFilesBucketUser(final List<ManagedPolicy> managedPolicies)
    {
        return User.Builder.create(this, "ContentFilesBucketUser")
                           .userName(String.format("%s-%s-s3", properties.customerName(), properties.environmentType().getName()))
                           .managedPolicies(managedPolicies)
                           .build();
    }

    private ManagedPolicy aceAccessPolicy(final Bucket contentFilesBucket)
    {
        List<PolicyStatement> policyStatements = new ArrayList<>();

        // RDS connection
        policyStatements.add(allow(String.format("arn:aws:rds-db:%s:%s:dbuser:%s/%s-%s", properties.region(),
                                                 properties.accountId(), properties.rdsResourceId(),
                                                 properties.customerName(), properties.environmentType().getName()),
                                   "rds-db:connect"));

        if (properties.environmentType() != DEV) {
            // Event bus events
            policyStatements.add(allow(properties.environmentType().getEventBusArn(), "events:PutEvents"));
        }

        // S3 bucket
//        policyStatements.addAll(List.of(allow("*", "s3:ListAllMyBuckets"),
//                                        allow(String.format("arn:aws:s3:::%s/*", contentFilesBucket.getBucketName()), "s3:PutObject", "s3:GetObject")));

        return ManagedPolicy.Builder.create(this, "ACEAccessPolicy")
                                    .managedPolicyName(String.format("%s-ace-access", properties.customerName()))
                                    .statements(policyStatements)
                                    .build();
    }

    private ManagedPolicy contentFilesBucketAccessPolicy(final Bucket bucket)
    {
        return ManagedPolicy.Builder.create(this, "ContentFilesBucketAccessPolicy")
                                    .managedPolicyName(String.format("%s-%s-s3-access-policy", properties.customerName(), properties.environmentType().getName()))
                                    .document(PolicyDocument.Builder.create()
                                                                    .statements(List.of(allow("*", "s3:ListAllMyBuckets"),
                                                                                        allow(String.format("arn:aws:s3:::%s/*", bucket.getBucketName()), "s3:PutObject", "s3:GetObject")))
                                                                    .build())
                                    .build();
    }

    private Bucket contentFilesBucket()
    {
        String bucketName = String.format("atex-cloud.%s-%s.files",
                                          properties.customerName(),
                                          properties.environmentType().getName());

        return Bucket.Builder.create(this, "ContentFilesBucket")
                             .bucketName(bucketName)
//                             .removalPolicy(RETAIN)
                             .versioned(false)
                             .encryption(S3_MANAGED)
                             .blockPublicAccess(BLOCK_ALL)
                             .lifecycleRules(List.of(LifecycleRule.builder()
                                                                  .id("tmp-lifecycle")
                                                                  .expiration(Duration.days(30))
                                                                  .prefix("/tmp")
                                                                  .enabled(true)
                                                                  .abortIncompleteMultipartUploadAfter(Duration.days(30))
                                                                  .build()))
                             .build();
    }
}
