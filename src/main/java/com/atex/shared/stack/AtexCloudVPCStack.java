package com.atex.shared.stack;

import com.atex.ace.EnvironmentType;
import com.atex.shared.configuration.VPCProperties;
import java.util.List;
import software.amazon.awscdk.Aspects;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tag;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.IpAddresses;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

import static software.amazon.awscdk.services.ec2.SubnetType.*;

public class AtexCloudVPCStack
    extends Stack
{
    public AtexCloudVPCStack(final Construct scope,
                             final String id,
                             final StackProps props,
                             final VPCProperties vpcProperties)
    {
        super(scope, id, props);

        Vpc atexCloudVPC = Vpc.Builder.create(this, "AtexCloudVPC")
                                      .vpcName(String.format("atex-cloud-%s", vpcProperties.customerName()))
                                      .enableDnsHostnames(true)
                                      .enableDnsSupport(true)
                                      .availabilityZones(List.of("eu-west-1a", "eu-west-1b"))
                                      .createInternetGateway(true)
                                      .ipAddresses(IpAddresses.cidr(vpcProperties.vpcCIDR()))
                                      .subnetConfiguration(List.of(SubnetConfiguration.builder()
                                                                                      .name(String.format("atex-cloud-%s-private",
                                                                                                          vpcProperties.customerName()))
                                                                                      .subnetType(PRIVATE_WITH_EGRESS) // This means NAT
                                                                                      .cidrMask(24)
                                                                                      .build(),
                                                                   SubnetConfiguration.builder()
                                                                                      .name(String.format("atex-cloud-%s-public",
                                                                                                          vpcProperties.customerName()))
                                                                                      .subnetType(PUBLIC) // Has to exist because of the NAT
                                                                                      .cidrMask(24)
                                                                                      .build()))
                                      .build();

        // VPC name tag, although I think this is already set...
        Aspects.of(atexCloudVPC).add(new Tag("Name", String.format("atex-cloud-%s", vpcProperties.customerName())));

        // Set the name tag of all the subnets. This will also set the name of associated
        // NAT gateways and route tables at the same time.

        for (ISubnet subnet : atexCloudVPC.getPublicSubnets()) {
            Aspects.of(subnet)
                   .add(new Tag("Name",
                                String.format("atex-cloud-%s-public-%s",
                                              vpcProperties.customerName(),
                                              subnet.getAvailabilityZone())));
        }

        for (ISubnet subnet : atexCloudVPC.getPrivateSubnets()) {
            Aspects.of(subnet)
                   .add(new Tag("Name",
                                String.format("atex-cloud-%s-private-%s",
                                              vpcProperties.customerName(),
                                              subnet.getAvailabilityZone())));
        }

        if (vpcProperties.environmentType() == EnvironmentType.PROD) {
            // TODO: if production flow logs should probable be enabled...
        }

        // TODO: is it possible to set the name tag of the main route table?

        // TODO: verify route tables
        // TODO: verify network ACLs

        // TODO: how to change the route table of the subnets?

        asOutput("AtexCloudVPCIdOutput", atexCloudVPC.getVpcId());
        asOutput("AtexCloudVPCInternetGatewayOutput", atexCloudVPC.getInternetGatewayId());

        List<ISubnet> privateSubnets = atexCloudVPC.getPrivateSubnets();

        for (int i = 0; i < atexCloudVPC.getPrivateSubnets().size(); i++) {
            asOutput(String.format("AtexCloudVPCPrivateSubnet%dIdOutput", i), privateSubnets.get(i).getSubnetId());
        }
    }

    protected void asOutput(final String outputName,
                            final String outputValue)
    {
        CfnOutput.Builder.create(this, outputName)
                         .exportName(outputName)
                         .value(outputValue)
                         .build();
    }
}
