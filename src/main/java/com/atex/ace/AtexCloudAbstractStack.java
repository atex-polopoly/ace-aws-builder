package com.atex.ace;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

public abstract class AtexCloudAbstractStack
    extends Stack
{
    public AtexCloudAbstractStack(final Construct scope,
                                  final String id,
                                  final StackProps props)
    {
        super(scope, id, props);
    }

    protected void asOutput(final String outputName,
                            final String outputValue)
    {
        CfnOutput.Builder.create(this, outputName)
                         .exportName(outputName)
                         .value(outputValue)
                         .build();
    }

    protected IHostedZone lookupHostedZone()
    {
        return HostedZone.fromHostedZoneAttributes(this, "dev.atexcloud.io",
                                                   HostedZoneAttributes.builder()
                                                                       .zoneName("dev.atexcloud.io")
                                                                       .hostedZoneId("Z02835961VQ8N8ROSFCY3") // this is dev...
                                                                       .build());
    }
}
