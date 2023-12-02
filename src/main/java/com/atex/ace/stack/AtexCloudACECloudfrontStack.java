package com.atex.ace.stack;

import com.atex.ace.CommonProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.StackProps;
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
import software.amazon.awscdk.services.cloudfront.IResponseHeadersPolicy;
import software.amazon.awscdk.services.cloudfront.OriginRequestCookieBehavior;
import software.amazon.awscdk.services.cloudfront.OriginRequestHeaderBehavior;
import software.amazon.awscdk.services.cloudfront.OriginRequestPolicy;
import software.amazon.awscdk.services.cloudfront.OriginRequestQueryStringBehavior;
import software.amazon.awscdk.services.cloudfront.ResponseCustomHeadersBehavior;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersPolicy;
import software.amazon.awscdk.services.cloudfront.origins.HttpOrigin;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

import static com.atex.ace.EnvironmentType.*;
import static software.amazon.awscdk.services.cloudfront.AllowedMethods.ALLOW_ALL;
import static software.amazon.awscdk.services.cloudfront.AllowedMethods.ALLOW_GET_HEAD;
import static software.amazon.awscdk.services.cloudfront.CachePolicy.*;
import static software.amazon.awscdk.services.cloudfront.OriginProtocolPolicy.HTTPS_ONLY;
import static software.amazon.awscdk.services.cloudfront.OriginProtocolPolicy.MATCH_VIEWER;
import static software.amazon.awscdk.services.cloudfront.OriginSslPolicy.*;
import static software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy.*;

/**
 * Stack that will construct all web delivery AWS resources
 * necessary for an ACE installation.
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

        // CF origin

        HttpOrigin aceApiOrigin = HttpOrigin.Builder.create(properties.loadBalancerDomain())
                                                    .originId("Atex Cloud rack ELB")
                                                    .protocolPolicy(HTTPS_ONLY)
                                                    .originSslProtocols(List.of(TLS_V1_2))
                                                    .build();

        HttpOrigin websiteOrigin = HttpOrigin.Builder.create(properties.loadBalancerDomain())
                                                     .originId("Atex Cloud rack ELB")
                                                     .protocolPolicy(MATCH_VIEWER)
                                                     .originSslProtocols(List.of(TLS_V1_2))
                                                     .build();

        // Policies

        // TODO: both of these policies should not be created in dev or staging since they already exist...

        ICachePolicy apiCachePolicy = cachePolicy();
        IOriginRequestPolicy apiOriginRequestPolicy = originRequestPolicy();
        IResponseHeadersPolicy apiResponseHeadersPolicy = responseHeadersPolicy();

//        if (properties.environmentType() == DEV || properties.environmentType() == STAGING) {
//            apiCachePolicy = CachePolicy.fromCachePolicyId(this, "ACEAPICachePolicy", "ACE-API-Cache");
//
//            apiOriginRequestPolicy = OriginRequestPolicy.fromOriginRequestPolicyId(this, "ACEAPIOriginRequestPolicy", "ACE-API-Origin");
//            apiResponseHeadersPolicy = ResponseHeadersPolicy.fromResponseHeadersPolicyId(this, "ACEAPIResponseHeadersPolicy", "ACE-API-Response");
//        } else {
//            apiCachePolicy = CachePolicy.Builder.create(this, "ACEAPICachePolicy")
//                                                .comment("ACE API cache policy")
//                                                .cachePolicyName("ACE-API-Cache")
//                                                .cookieBehavior(CacheCookieBehavior.none())
//                                                .headerBehavior(CacheHeaderBehavior.none())
//                                                .queryStringBehavior(CacheQueryStringBehavior.all())
//                                                .enableAcceptEncodingBrotli(true)
//                                                .enableAcceptEncodingGzip(true)
//                                                .minTtl(Duration.seconds(0))
//                                                .maxTtl(Duration.days(365))
//                                                .defaultTtl(Duration.seconds(0))
//                                                .build();
//
//            apiOriginRequestPolicy = OriginRequestPolicy.Builder.create(this, "ACEAPIOriginRequestPolicy")
//                                                                .comment("ACE API origin request policy")
//                                                                .originRequestPolicyName("ACE-API-Origin")
//                                                                .cookieBehavior(OriginRequestCookieBehavior.none())
//                                                                .headerBehavior(OriginRequestHeaderBehavior.all())
//                                                                .queryStringBehavior(OriginRequestQueryStringBehavior.all())
//                                                                .build();
//
//            apiResponseHeadersPolicy = ResponseHeadersPolicy.Builder.create(this, "ACEAPIResponseHeadersPolicy")
//                                                                    .comment("ACE API origin response header policy")
//                                                                    .responseHeadersPolicyName("ACE-API-Response")
//                                                                    .customHeadersBehavior(ResponseCustomHeadersBehavior.builder().customHeaders(List.of()).build())
//                                                                    .build();
//        }

        Distribution apiDistribution = createApiDistribution(aceApiOrigin, apiOriginRequestPolicy, apiResponseHeadersPolicy, apiCachePolicy, hostedZone);
        Distribution aceCustomerWebsite = createWebsiteDistribution(websiteOrigin, hostedZone);

        dnsEntry(apiDomainName(), apiDistribution.getDistributionDomainName(), hostedZone);
        dnsEntry(websiteDomainName(), aceCustomerWebsite.getDistributionDomainName(), hostedZone);

        // TODO: lambda@edge?
    }

    private Distribution createApiDistribution(final IOrigin origin,
                                               final IOriginRequestPolicy originRequestPolicy,
                                               final IResponseHeadersPolicy responseHeadersPolicy,
                                               final ICachePolicy cachePolicy,
                                               final IHostedZone hostedZone)
    {
        ICertificate cloudfrontApiCertificate = certificate("APICertificate", apiDomainName(), hostedZone);

        Map<String, BehaviorOptions> behaviours = new HashMap<>();

        behaviours.put("/image-service/*", BehaviorOptions.builder()
                                                          .origin(origin)
                                                          .compress(false)
                                                          .viewerProtocolPolicy(REDIRECT_TO_HTTPS)
                                                          .allowedMethods(AllowedMethods.ALLOW_GET_HEAD)
                                                          .cachePolicy(CachePolicy.Builder.create(this, "ACEAPIImageServiceBehaviourCachePolicy")
                                                                                          .cachePolicyName("ACEAPIImageServiceBehaviourCachePolicy")
                                                                                          .enableAcceptEncodingBrotli(true)
                                                                                          .enableAcceptEncodingGzip(true)
                                                                                          .queryStringBehavior(CacheQueryStringBehavior.all())
                                                                                          .headerBehavior(CacheHeaderBehavior.allowList("Host"))
                                                                                          .cookieBehavior(CacheCookieBehavior.none())
                                                                                          .build())
                                                          .build());

        // TODO: origin response policy...

        behaviours.put("/content-service/*", BehaviorOptions.builder()
                                                            .origin(origin)
                                                            .compress(true)
                                                            .viewerProtocolPolicy(REDIRECT_TO_HTTPS)
                                                            .allowedMethods(ALLOW_ALL)
                                                            .originRequestPolicy(originRequestPolicy)
                                                            .responseHeadersPolicy(responseHeadersPolicy)
                                                            .cachePolicy(cachePolicy)
                                                            .build());

        // TODO: origin response policy...

        return Distribution.Builder.create(this, "APIDistribution")
                                   .comment(String.format("%s %s API", properties.customerName(), properties.environmentType().getName()))
                                   .domainNames(List.of(apiDomainName()))
                                   .certificate(cloudfrontApiCertificate)
                                   .defaultBehavior(BehaviorOptions.builder()
                                                                   .origin(origin)
                                                                   .compress(true)
                                                                   .viewerProtocolPolicy(REDIRECT_TO_HTTPS)
                                                                   .allowedMethods(ALLOW_ALL)
                                                                   .originRequestPolicy(originRequestPolicy)
                                                                   .responseHeadersPolicy(responseHeadersPolicy)
                                                                   .cachePolicy(CACHING_DISABLED)
                                                                   .build())
                                   .additionalBehaviors(behaviours)
                                   .build();
    }

    private Distribution createWebsiteDistribution(final IOrigin origin,
                                                   final IHostedZone hostedZone)
    {
        ICertificate cloudfrontWebsiteCertificate = certificate("WebsiteCertificate", websiteDomainName(), hostedZone);

        return Distribution.Builder.create(this, "WebsiteDistribution")
                                   .comment(String.format("%s %s", properties.customerName(), properties.environmentType().getName()))
                                   .domainNames(List.of(websiteDomainName()))
                                   .certificate(cloudfrontWebsiteCertificate)
                                   .defaultBehavior(BehaviorOptions.builder()
                                                                   .origin(origin)
                                                                   .compress(true)
                                                                   .viewerProtocolPolicy(REDIRECT_TO_HTTPS)
                                                                   .allowedMethods(ALLOW_GET_HEAD)
                                                                   .cachePolicy(CachePolicy.Builder.create(this, "ACEWebDefaultCacheBehaviour")
                                                                                                   .cachePolicyName("ACEWebDefaultCacheBehaviour")
                                                                                                   .minTtl(Duration.seconds(1))
                                                                                                   .maxTtl(Duration.days(365))
                                                                                                   .defaultTtl(Duration.days(1))
                                                                                                   .queryStringBehavior(CacheQueryStringBehavior.allowList("previewData", "q", "amp", "page", "debugAds", "debugRequest"))
                                                                                                   .headerBehavior(CacheHeaderBehavior.allowList("Origin", "CloudFront-Is-Tablet-Viewer", "CloudFront-Is-Mobile-Viewer", "Host", "CloudFront-Is-Desktop-Viewer"))
                                                                                                   .cookieBehavior(CacheCookieBehavior.none())
                                                                                                   .enableAcceptEncodingGzip(true)
                                                                                                   .enableAcceptEncodingBrotli(true)
                                                                                                   .build())
                                                                   .build())
                                   .build();
    }

    private ICachePolicy cachePolicy()
    {
        if (properties.environmentType() == DEV || properties.environmentType() == STAGING) {
            return CachePolicy.fromCachePolicyId(this, "ACEAPICachePolicy", properties.environmentType().getCloudfrontCachePolicyId());
        }

        return CachePolicy.Builder.create(this, "ACEAPICachePolicy")
                                  .comment("ACE API cache policy")
                                  .cachePolicyName("ACE-API-Cache")
                                  .cookieBehavior(CacheCookieBehavior.none())
                                  .headerBehavior(CacheHeaderBehavior.none())
                                  .queryStringBehavior(CacheQueryStringBehavior.all())
                                  .enableAcceptEncodingBrotli(true)
                                  .enableAcceptEncodingGzip(true)
                                  .minTtl(Duration.seconds(0))
                                  .maxTtl(Duration.days(365))
                                  .defaultTtl(Duration.seconds(0))
                                  .build();
    }

    private IOriginRequestPolicy originRequestPolicy()
    {
        if (properties.environmentType() == DEV || properties.environmentType() == STAGING) {
            return OriginRequestPolicy.fromOriginRequestPolicyId(this, "ACEAPIOriginRequestPolicy", properties.environmentType().getCloudfrontOriginRequestPolicyId());
        }

        return OriginRequestPolicy.Builder.create(this, "ACEAPIOriginRequestPolicy")
                                          .comment("ACE API origin request policy")
                                          .originRequestPolicyName("ACE-API-Origin")
                                          .cookieBehavior(OriginRequestCookieBehavior.none())
                                          .headerBehavior(OriginRequestHeaderBehavior.all())
                                          .queryStringBehavior(OriginRequestQueryStringBehavior.all())
                                          .build();
    }

    private IResponseHeadersPolicy responseHeadersPolicy()
    {
        if (properties.environmentType() == DEV || properties.environmentType() == STAGING) {
            return ResponseHeadersPolicy.fromResponseHeadersPolicyId(this, "ACEAPIResponseHeadersPolicy", properties.environmentType().getCloudfrontResponseHeadersPolicyId());
        }


        return ResponseHeadersPolicy.Builder.create(this, "ACEAPIResponseHeadersPolicy")
                                            .comment("ACE API origin response header policy")
                                            .responseHeadersPolicyName("ACE-API-Response")
                                            .customHeadersBehavior(ResponseCustomHeadersBehavior.builder().customHeaders(List.of()).build())
                                            .build();
    }
}
