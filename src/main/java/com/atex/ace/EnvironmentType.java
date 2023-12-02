package com.atex.ace;

public enum EnvironmentType
{
    DEV("dev", "8126ba2e-62cc-4e25-809d-8160c63edd51", "f9ec7a7f-cad6-4052-8863-921fad2f637c", "e3e518f8-a00f-4082-9d3d-d834e9b7bc07", "Z02835961VQ8N8ROSFCY3", "dev.atexcloud.io"),
    STAGING("staging", "???", "???", "???", "???", "staging.atexcloud.io"),

    PROD("prod", null, null, null, null, "atexcloud.io");

    private String name;

    private String hostedZoneId;
    private String hostedZoneName;

    private String cloudfrontCachePolicyId;
    private String cloudfrontOriginRequestPolicyId;
    private String cloudfrontResponseHeadersPolicyId;

    EnvironmentType()
    {

    }

    EnvironmentType(final String name,
                    final String cloudfrontCachePolicyId,
                    final String cloudfrontOriginRequestPolicyId,
                    final String cloudfrontResponseHeadersPolicyId,
                    final String hostedZoneId,
                    final String hostedZoneName)
    {
        this.name = name;

        this.cloudfrontCachePolicyId = cloudfrontCachePolicyId;
        this.cloudfrontOriginRequestPolicyId = cloudfrontOriginRequestPolicyId;
        this.cloudfrontResponseHeadersPolicyId = cloudfrontResponseHeadersPolicyId;

        this.hostedZoneId = hostedZoneId;
        this.hostedZoneName = hostedZoneName;
    }

    public String getName()
    {
        return name;
    }

    public String getHostedZoneId()
    {
        return hostedZoneId;
    }

    public String getHostedZoneName()
    {
        return hostedZoneName;
    }

    public String getCloudfrontCachePolicyId()
    {
        return cloudfrontCachePolicyId;
    }

    public String getCloudfrontOriginRequestPolicyId()
    {
        return cloudfrontOriginRequestPolicyId;
    }

    public String getCloudfrontResponseHeadersPolicyId()
    {
        return cloudfrontResponseHeadersPolicyId;
    }
}
