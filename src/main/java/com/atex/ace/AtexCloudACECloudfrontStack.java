package com.atex.ace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.cloudfront.AllowedMethods;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CacheCookieBehavior;
import software.amazon.awscdk.services.cloudfront.CacheHeaderBehavior;
import software.amazon.awscdk.services.cloudfront.CachePolicy;
import software.amazon.awscdk.services.cloudfront.CacheQueryStringBehavior;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.ICachePolicy;
import software.amazon.awscdk.services.cloudfront.IOrigin;
import software.amazon.awscdk.services.cloudfront.IOriginRequestPolicy;
import software.amazon.awscdk.services.cloudfront.OriginRequestCookieBehavior;
import software.amazon.awscdk.services.cloudfront.OriginRequestHeaderBehavior;
import software.amazon.awscdk.services.cloudfront.OriginRequestPolicy;
import software.amazon.awscdk.services.cloudfront.OriginRequestQueryStringBehavior;
import software.amazon.awscdk.services.cloudfront.origins.HttpOrigin;
import software.amazon.awscdk.services.route53.CnameRecord;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

/**
 * Stack that will construct the AWS resources necessary for all web delivery.
 * This will include:
 *
 * - Standard Atex Cloud Cloudfront policies
 * - Cloudfront API distribution
 * - Cloudfront website distribution
 * - Certificates
 * - DNS entries
 */
public class AtexCloudACECloudfrontStack
    extends AtexCloudAbstractStack
{
    public AtexCloudACECloudfrontStack(final Construct scope,
                                       final String id,
                                       final StackProps props,
                                       final CommonProperties properties)
    {
        super(scope, id, props, properties);

        IHostedZone hostedZone = lookupHostedZone();

        // CF origin

        HttpOrigin apiOrigin = HttpOrigin.Builder.create(properties.loadBalancerDomain())
                                                 .originId("Atex Cloud rack ELB")
                                                 .build();

        // Policies

        OriginRequestPolicy apiOriginRequestPolicy = OriginRequestPolicy.Builder.create(this, "ace-api-origin-request-policy")
                                                                                .comment("ACE API origin request policy")
                                                                                .originRequestPolicyName("ACE-API-Origin-2")
                                                                                .cookieBehavior(OriginRequestCookieBehavior.none())
                                                                                .headerBehavior(OriginRequestHeaderBehavior.all())
                                                                                .queryStringBehavior(OriginRequestQueryStringBehavior.all())
                                                                                .build();

        CachePolicy apiCachePolicy = CachePolicy.Builder.create(this, "ace-api-cache-policy")
                                                        .comment("ACE API cache policy")
                                                        .cachePolicyName("ACE-API-Cache-2")
                                                        .cookieBehavior(CacheCookieBehavior.none())
                                                        .headerBehavior(CacheHeaderBehavior.none())
                                                        .queryStringBehavior(CacheQueryStringBehavior.all())
                                                        .enableAcceptEncodingBrotli(true)
                                                        .enableAcceptEncodingGzip(true)
                                                        .minTtl(Duration.seconds(0))
                                                        .maxTtl(Duration.days(365))
                                                        .defaultTtl(Duration.seconds(0))
                                                        .build();

        // TODO: response headers policy...

        Distribution apiDistribution = createApiDistribution(apiOrigin, apiOriginRequestPolicy, apiCachePolicy, hostedZone);
        Distribution aceCustomerWebsite = createWebsiteDistribution(apiOrigin, apiOriginRequestPolicy, apiCachePolicy, hostedZone);

        CName(apiDistribution.getDistributionDomainName(), hostedZone);
        CName(aceCustomerWebsite.getDistributionDomainName(), hostedZone);
    }

    private Distribution createApiDistribution(final IOrigin origin,
                                               final IOriginRequestPolicy originRequestPolicy,
                                               final ICachePolicy cachePolicy,
                                               final IHostedZone hostedZone)
    {
        ICertificate cloudfrontApiCertificate = Certificate.Builder.create(this, "api-cloudfront-customer-cert")
                                                                   .domainName(String.format("api.%s.dev.atexcloud.io", properties.customerName()))
                                                                   .certificateName("api-cloudfront-customer-cert")
                                                                   .validation(CertificateValidation.fromDns(hostedZone))
                                                                   .build();

        Map<String, BehaviorOptions> behaviours = new HashMap<>();

        behaviours.put("/image-service/*", BehaviorOptions.builder()
                                                          .origin(origin)
                                                          .compress(false)
                                                          .allowedMethods(AllowedMethods.ALLOW_GET_HEAD)
                                                          .cachePolicy(CachePolicy.CACHING_DISABLED)
                                                          .build());

        behaviours.put("/content-service/*", BehaviorOptions.builder()
                                                            .origin(origin)
                                                            .compress(true)
                                                            .allowedMethods(AllowedMethods.ALLOW_ALL)
                                                            .originRequestPolicy(originRequestPolicy)
                                                            .cachePolicy(cachePolicy)
                                                            .build());

        return Distribution.Builder.create(this, "api-customer-cloudfront")
                                   .comment(String.format("%s dev API", properties.customerName()))
                                   .domainNames(List.of(String.format("api.%s.dev.atexcloud.io", properties.customerName())))
                                   .certificate(cloudfrontApiCertificate)
                                   .defaultBehavior(BehaviorOptions.builder()
                                                                   .origin(origin)
                                                                   .originRequestPolicy(originRequestPolicy)
                                                                   .cachePolicy(cachePolicy)
                                                                   .build())
                                   .additionalBehaviors(behaviours)
                                   .build();
    }

    private CnameRecord CName(final String domainName,
                              final IHostedZone hostedZone)
    {
        return CnameRecord.Builder.create(this, String.format("api.%s.dev.atexcloud.io", properties.customerName()))
                                  .recordName(String.format("api.%s.dev.atexcloud.io", properties.customerName()))
                                  .domainName(domainName)
                                  .ttl(Duration.minutes(5))
                                  .zone(hostedZone)
                                  .build();
    }

    private Distribution createWebsiteDistribution(final IOrigin origin,
                                                   final IOriginRequestPolicy originRequestPolicy,
                                                   final ICachePolicy cachePolicy,
                                                   final IHostedZone hostedZone)
    {
        ICertificate cloudfrontWebsiteCertificate = Certificate.Builder.create(this, "website-cloudfront-customer-cert")
                                                                       .domainName(String.format("%s.dev.atexcloud.io", properties.customerName()))
                                                                       .certificateName("website-cloudfront-customer-cert")
                                                                       .validation(CertificateValidation.fromDns(hostedZone))
                                                                       .build();

        return Distribution.Builder.create(this, "api-customer-website-cloudfront")
                                   .comment(String.format("%s dev", properties.customerName()))
                                   .domainNames(List.of(String.format("%s.dev.atexcloud.io", properties.customerName())))
                                   .certificate(cloudfrontWebsiteCertificate)
                                   .defaultBehavior(BehaviorOptions.builder()
                                                                   .origin(origin)
                                                                   .originRequestPolicy(originRequestPolicy)
                                                                   .cachePolicy(cachePolicy)
                                                                   .build())
                                   .build();
    }
}
