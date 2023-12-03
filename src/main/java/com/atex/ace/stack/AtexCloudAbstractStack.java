package com.atex.ace.stack;

import com.atex.ace.CommonProperties;
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

import static com.atex.ace.EnvironmentType.*;

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
        if (properties.environmentType() == PROD) {
            // This would be possible to do without e-mail validation if we delegated also production
            // atexcloud.io subdomains (like zawya.atexcloud.io) to the production account.

            return Certificate.Builder.create(this, certificateName)
                                      .domainName(domainName)
                                      .certificateName(certificateName)
                                      .validation(CertificateValidation.fromEmail())
                                      .build();
        }

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
        return String.format("api.%s.%s",
                             properties.customerName(),
                             properties.environmentType().getZoneDetails().zoneName());
    }

    protected String sitemapDomainName()
    {
        return String.format("sitemap.%s.%s",
                             properties.customerName(),
                             properties.environmentType().getZoneDetails().zoneName());
    }

    protected String websiteDomainName()
    {
        return String.format("%s.%s", properties.customerName(),
                             properties.environmentType().getZoneDetails().zoneName());
    }

    private IHostedZone lookupHostedZone()
    {
        if (properties.environmentType() == PROD) {
            // This would be possible to do in prod as well if we delegated also production
            // atexcloud.io subdomains (like zawya.atexcloud.io) to the production account.

            return null;
        }

        return HostedZone.fromHostedZoneAttributes(this, "HostedZone",
                                                   HostedZoneAttributes.builder()
                                                                       .hostedZoneId(properties.environmentType().getZoneDetails().zoneId())
                                                                       .zoneName(properties.environmentType().getZoneDetails().zoneName())
                                                                       .build());
    }
}
