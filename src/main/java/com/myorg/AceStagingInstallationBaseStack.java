package com.myorg;

import java.util.List;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.iam.CfnAccessKey;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class AceStagingInstallationBaseStack
    extends Stack
{
    public AceStagingInstallationBaseStack(final Construct scope,
                                           final String id)
    {
        this(scope, id, null);
    }

    public AceStagingInstallationBaseStack(final Construct scope,
                                           final String id,
                                           final StackProps props)
    {
        super(scope, id, props);

        // Inputs

        String customerName = (String) this.getNode().tryGetContext("customer-name");

        if (customerName == null || customerName.trim().length() == 0) {
            throw new RuntimeException("Customer name ('customer-name') is a required input!");
        }

        String databaseClusterARN = (String) this.getNode().tryGetContext("database-arn");

        if (databaseClusterARN == null || databaseClusterARN.trim().length() == 0) {
            throw new RuntimeException("Database ARN ('database-arn') is a required input!");
        }

        // End inputs

        // S3 bucket

        Bucket aceContentFilesBucket = Bucket.Builder.create(this, "AceContentFilesBucket")
                                                     .bucketName(String.format("atex-cloud.%s-staging.files", customerName))
                                                     .versioned(false)
                                                     .build();

        CfnOutput.Builder.create(this, "AceContentFilesBucketName")
                         .exportName("AceContentFilesBucketName")
                         .value(aceContentFilesBucket.getBucketName())
                         .build();

        // TODO: Lookup RDS cluster. Doesn't seem you can do it only based on for example ARN (need to supply all properties)?

        // IAM ACE access policy

        ManagedPolicy aceAccessManagedPolicy = ManagedPolicy.Builder.create(this, "AceAccessPolicy")
                                                                    .managedPolicyName(String.format("%s-ace-access", customerName))
                                                                    .statements(List.of(PolicyStatement.Builder.create()
                                                                                                               .effect(Effect.ALLOW)
                                                                                                               .actions(List.of("rds-db:connect"))
                                                                                                               .resources(List.of(String.format("%s/%s-staging", databaseClusterARN, customerName)))
                                                                                                               .build(),
                                                                                        PolicyStatement.Builder.create()
                                                                                                               .effect(Effect.ALLOW)
                                                                                                               .actions(List.of("events:PutEvents"))
                                                                                                               .resources(List.of("arn:aws:events:eu-west-1:103826127765:event-bus/cms-events-staging"))
                                                                                                               .build()))
                                                                    .build();

        CfnOutput.Builder.create(this, "AceAccessPolicyARN")
                         .exportName("AceAccessPolicyARN")
                         .value(aceAccessManagedPolicy.getManagedPolicyArn())
                         .build();

        // IAM user (until we no longer need it)

        ManagedPolicy aceContentFilesAccessPolicy = ManagedPolicy.Builder.create(this, "AceContentFilesBucketAcessPolicy")
                                                                         .managedPolicyName(String.format("%s-s3-access-policy", customerName))
                                                                         .document(PolicyDocument.Builder.create()
                                                                                                         .statements(List.of(PolicyStatement.Builder.create()
                                                                                                                                                    .effect(Effect.ALLOW)
                                                                                                                                                    .actions(List.of("s3:ListAllMyBuckets"))
                                                                                                                                                    .resources(List.of("*"))
                                                                                                                                                    .build(),
                                                                                                                             PolicyStatement.Builder.create()
                                                                                                                                                    .effect(Effect.ALLOW)
                                                                                                                                                    .actions(List.of("s3:PutObject", "s3:GetObject"))
                                                                                                                                                    .resources(List.of(String.format("arn:aws:s3:::%s/*", aceContentFilesBucket.getBucketName())))
                                                                                                                                                    .build()))
                                                                                                         .build())
                                                                         .build();

        User aceContentFilesBucketUser = User.Builder.create(this, "AceContentFilesBucketUser")
                                                     .userName(String.format("%s-s3", customerName))
                                                     .managedPolicies(List.of(aceContentFilesAccessPolicy))
                                                     .build();

        CfnAccessKey aceContentFilesBucketUserAccssKey = CfnAccessKey.Builder.create(this, "AceContentFilesBucketAccessKey")
                                                                             .userName(aceContentFilesBucketUser.getUserName())
                                                                             .build();

        CfnOutput.Builder.create(this, "AceContentFilesBucketAccessKeyValue")
                         .exportName("AceContentFilesBucketAccessKeyValue")
                         .value(aceContentFilesBucketUserAccssKey.getRef())
                         .build();

        CfnOutput.Builder.create(this, "AceContentFilesBucketSecretKeyValue")
                         .exportName("AceContentFilesBucketSecretKeyValue")
                         .value(aceContentFilesBucketUserAccssKey.getAttrSecretAccessKey())
                         .build();

//        // DNS entries
//        IHostedZone stagingZone = HostedZone.fromHostedZoneAttributes(this, "staging.atexcloud.io",
//                                                                      HostedZoneAttributes.builder()
//                                                                                          .zoneName("staging.atexcloud.io")
//                                                                                          .hostedZoneId("Z09900291NP6WQSDNBAR0")
//                                                                                          .build());
//
//        CnameRecord.Builder.create(this, "customer.staging.atexcloud.io")
//                           .recordName("customer.staging.atexcloud.io")
//                           .domainName("stagi-Route-17INL3KHIYRSU-983353251.eu-west-1.elb.amazonaws.com")
//                           .zone(stagingZone)
//                           .build();
//
//        CnameRecord.Builder.create(this, "api.customer.staging.atexcloud.io")
//                           .recordName("api.staging.atexcloud.io")
//                           .domainName("stagi-Route-17INL3KHIYRSU-983353251.eu-west-1.elb.amazonaws.com")
//                           .zone(stagingZone)
//                           .build();
//
//        CnameRecord.Builder.create(this, "sitemap.customer.staging.atexcloud.io")
//                           .recordName("sitemap.staging.atexcloud.io")
//                           .domainName("stagi-Route-17INL3KHIYRSU-983353251.eu-west-1.elb.amazonaws.com")
//                           .zone(stagingZone)
//                           .build();

        // TODO: certs...

        // TODO: cloudfront?
    }
}
