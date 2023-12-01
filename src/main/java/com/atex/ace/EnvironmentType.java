package com.atex.ace;

public enum EnvironmentType
{
    DEV("dev", "Z02835961VQ8N8ROSFCY3", "dev.atexcloud.io"),
    STAGING("staging", "???", "???"),

    PROD;

    private String name;

    private String hostedZoneId;
    private String hostedZoneName;

    EnvironmentType()
    {

    }

    EnvironmentType(final String name,
                    final String hostedZoneId,
                    final String hostedZoneName)
    {
        this.name = name;

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
}
