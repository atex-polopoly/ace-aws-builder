package com.atex.shared;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.rds.AuroraMysqlClusterEngineProps;
import software.amazon.awscdk.services.rds.ClusterInstance;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseCluster;
import software.amazon.awscdk.services.rds.DatabaseClusterEngine;
import software.amazon.awscdk.services.rds.IClusterEngine;
import software.amazon.awscdk.services.rds.IClusterInstance;
import software.amazon.awscdk.services.rds.ParameterGroup;
import software.amazon.awscdk.services.rds.ProvisionedClusterInstanceProps;
import software.amazon.awscdk.services.rds.SubnetGroup;
import software.constructs.Construct;

import static software.amazon.awscdk.services.rds.AuroraMysqlEngineVersion.*;

public class AtexCloudRDSStack
    extends Stack
{
    public AtexCloudRDSStack(final Construct scope,
                             final String id,
                             final StackProps props,
                             final RDSProperties rdsProperties)
    {
        super(scope, id, props);

        // Lookups

        IVpc iVpc = Vpc.fromLookup(this, "AtexCloudVPC", VpcLookupOptions.builder()
                                                                         .vpcId(rdsProperties.vpcId())
                                                                         .build());

        List<ISubnet> privateSubnets = iVpc.getPrivateSubnets();

        ISubnet subnet1 = findSubnet(rdsProperties.subnet1Id(), privateSubnets);
        ISubnet subnet2 = findSubnet(rdsProperties.subnet2Id(), privateSubnets);

        if (subnet1 == null || subnet2 == null) {
            throw new RuntimeException(String.format("At least one of the supplied subnets does not exist in the supplied '%s' VPC, "
                                                     + "please make sure both subnets are private subnets in it!",
                                                     rdsProperties.vpcId()));
        }

        // Database engine

        IClusterEngine databaseEngine = DatabaseClusterEngine.auroraMysql(AuroraMysqlClusterEngineProps.builder()
                                                                                                       .version(VER_3_05_0)
                                                                                                       .build());

        // Parameter group

        ParameterGroup parameterGroup = ParameterGroup.Builder.create(this, "AtexCloudRDSClusterParameterGroup")
                                                              .description("DB cluster parameters for Atex Cloud MYSQL")
                                                              .engine(databaseEngine)
                                                              .parameters(Map.of("binlog_format", "ROW"))
                                                              .build();

        // Security group

        SecurityGroup securityGroup = SecurityGroup.Builder.create(this, "AtexCloudRDSSecurityGroup")
                                                           .securityGroupName("atex-cloud-rds-sg")
                                                           .description("Atex Cloud RDS security group")
                                                           .vpc(iVpc)
                                                           .allowAllOutbound(true)
                                                           .build();

        securityGroup.addIngressRule(Peer.ipv4("10.250.250.250/32"), Port.allTraffic(), "All traffic from Atex VPN");
        securityGroup.addIngressRule(Peer.ipv4(iVpc.getVpcCidrBlock()), Port.allTraffic(), "All traffic from VPC");

        // Subnet group

        SubnetGroup subnetGroup = SubnetGroup.Builder.create(this, "AtexCloudRDSSubnetGroup")
                                                     .subnetGroupName("atex-cloud-rds-subnet-group")
                                                     .description("Possible subnets for Atex Cloud RDS")
                                                     .vpc(iVpc)
                                                     .vpcSubnets(SubnetSelection.builder()
                                                                                .subnets(List.of(subnet1, subnet2))
                                                                                .build())
                                                     .build();

        IClusterInstance clusterInstance = ClusterInstance.provisioned("ClusterInstance", ProvisionedClusterInstanceProps.builder()
                                                                                                                         .instanceType(InstanceType.of(InstanceClass.T4G,
                                                                                                                                                       InstanceSize.MEDIUM))
                                                                                                                         .build());

        DatabaseCluster rdsCluster = DatabaseCluster.Builder.create(this, "AtexCloudRDSCluster")
                                                            .clusterIdentifier(String.format("atex-cloud-%s-%s", rdsProperties.customerName(), rdsProperties.environmentType().getName()))
                                                            .vpc(iVpc)
                                                            .credentials(Credentials.fromGeneratedSecret("ace_admin"))
                                                            .subnetGroup(subnetGroup)
                                                            .parameterGroup(parameterGroup)
                                                            .engine(databaseEngine)
                                                            .backtrackWindow(Duration.days(1))
                                                            .iamAuthentication(true)
                                                            .writer(clusterInstance)
                                                            .securityGroups(List.of(securityGroup))
                                                            .build();

        asOutput("AtexCloudRDSClusterAdminSecret", rdsCluster.getSecret().getSecretValue().unsafeUnwrap());
        asOutput("AtexCloudRDSClusterResourceIdOutput", rdsCluster.getClusterResourceIdentifier());
    }

    private ISubnet findSubnet(final String subnetId,
                               final List<ISubnet> subnets)
    {
        for (ISubnet subnet : subnets) {
            if (subnet.getSubnetId().equals(subnetId)) {
                return subnet;
            }
        }

        return null;
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
