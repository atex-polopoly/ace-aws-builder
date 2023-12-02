package com.atex.ace;

public enum EnvironmentType
{
    DEV("dev", new HostedZoneDetails("Z02835961VQ8N8ROSFCY3", "dev.atexcloud.io"), new CloudfrontDetails("8126ba2e-62cc-4e25-809d-8160c63edd51", "f9ec7a7f-cad6-4052-8863-921fad2f637c", "e3e518f8-a00f-4082-9d3d-d834e9b7bc07")),
    STAGING("staging", new HostedZoneDetails(null, "staging.atexcloud.io"), new CloudfrontDetails("???", "???", "???")),

    PROD("prod", new HostedZoneDetails(null, "atexcloud.io"), new CloudfrontDetails(null, null, null));

    private final String name;

    private final HostedZoneDetails zoneDetails;
    private final CloudfrontDetails cloudfrontDetails;

    EnvironmentType(final String name,
                    final HostedZoneDetails zoneDetails,
                    final CloudfrontDetails cloudfrontDetails)
    {
        this.name = name;

        this.zoneDetails = zoneDetails;
        this.cloudfrontDetails = cloudfrontDetails;
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
}
