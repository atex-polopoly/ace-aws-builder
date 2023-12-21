package com.atex.ace.stack;

import com.atex.ace.CommonProperties;
import com.atex.ace.EnvironmentType;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.iam.CfnAccessKey;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.route53.CnameRecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

public abstract class AtexCloudAbstractStack
    extends Stack
{
    protected final CommonProperties properties;

    protected final IHostedZone hostedZone;

    public AtexCloudAbstractStack(final Construct scope,
                                  final String id,
                                  final StackProps props,
                                  final CommonProperties properties)
    {
        super(scope, id, props);

        this.properties = properties;

        // We look up all shared resources here
        this.hostedZone = lookupHostedZone();
        // TODO: Lookup RDS cluster. Doesn't seem you can do it only based on for example ARN (need to supply all properties)?
    }

    protected void asOutput(final String outputName,
                            final String outputValue)
    {
        CfnOutput.Builder.create(this, outputName)
                         .exportName(outputName)
                         .value(outputValue)
                         .build();
    }

    protected CnameRecord dnsEntry(final String entryName,
                                   final String entryValue,
                                   final IHostedZone hostedZone)
    {
        return CnameRecord.Builder.create(this, entryName)
                                  .recordName(entryName)
                                  .domainName(entryValue)
                                  .ttl(Duration.minutes(5))
                                  .zone(hostedZone)
                                  .build();
    }

    protected Certificate certificate(final String certificateName,
                                      final String domainName,
                                      final IHostedZone hostedZone)
    {
        return Certificate.Builder.create(this, certificateName)
                                  .domainName(domainName)
                                  .certificateName(certificateName)
                                  .validation(CertificateValidation.fromDns(hostedZone))
                                  .build();
    }

    protected CfnAccessKey accessKey(final String name,
                                     final User user)
    {
        return CfnAccessKey.Builder.create(this, name)
                                   .userName(user.getUserName())
                                   .build();
    }

    protected String apiDomainName()
    {
        return String.format("api%s.%s",
                             properties.environmentType() != EnvironmentType.PROD ? "." + properties.customerName() : "",
                             properties.hostedZoneDetails().zoneName());
    }

    protected String sitemapDomainName()
    {
        return String.format("sitemap.%s.%s",
                             properties.environmentType() != EnvironmentType.PROD ? "." + properties.customerName() : "",
                             properties.hostedZoneDetails().zoneName());
    }

    protected String websiteDomainName()
    {
        return String.format("%s.%s", properties.customerName(),
                             properties.hostedZoneDetails().zoneName());
    }

    private IHostedZone lookupHostedZone()
    {
        return HostedZone.fromHostedZoneAttributes(this, "HostedZone",
                                                   HostedZoneAttributes.builder()
                                                                       .hostedZoneId(properties.hostedZoneDetails().zoneId())
                                                                       .zoneName(properties.hostedZoneDetails().zoneName())
                                                                       .build());
    }
}
