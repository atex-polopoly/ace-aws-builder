package com.atex.ace;

public enum EnvironmentType
{
    DEV("dev",
        new HostedZoneDetails("Z02835961VQ8N8ROSFCY3", "dev.atexcloud.io"),
        new CloudfrontDetails("8126ba2e-62cc-4e25-809d-8160c63edd51", "f9ec7a7f-cad6-4052-8863-921fad2f637c", "e3e518f8-a00f-4082-9d3d-d834e9b7bc07"),
        null),

    STAGING("staging",
            new HostedZoneDetails(null, "staging.atexcloud.io"),
            new CloudfrontDetails("???", "???", "???"),
            "arn:aws:events:eu-west-1:103826127765:event-bus/cms-events-staging"),

    PROD("prod",
         new HostedZoneDetails(null, "atexcloud.io"),
         new CloudfrontDetails(null, null, null),
         "arn:aws:events:eu-west-1:392381418889:event-bus/cms-events");

    private final String name;

    private final HostedZoneDetails zoneDetails;
    private final CloudfrontDetails cloudfrontDetails;

    private final String eventBusArn;

    EnvironmentType(final String name,
                    final HostedZoneDetails zoneDetails,
                    final CloudfrontDetails cloudfrontDetails,
                    final String eventBusArn)
    {
        this.name = name;

        this.zoneDetails = zoneDetails;
        this.cloudfrontDetails = cloudfrontDetails;

        this.eventBusArn = eventBusArn;
    }

    public String getName()
    {
        return name;
    }

    public HostedZoneDetails getZoneDetails()
    {
        return zoneDetails;
    }

    public CloudfrontDetails getCloudfrontDetails()
    {
        return cloudfrontDetails;
    }

    public String getEventBusArn()
    {
        return eventBusArn;
    }

    public static EnvironmentType fromString(final String envTypeString)
    {
        for (EnvironmentType type : values()) {
            if (type.getName().equals(envTypeString)) {
                return type;
            }
        }

        return null;
    }
}
