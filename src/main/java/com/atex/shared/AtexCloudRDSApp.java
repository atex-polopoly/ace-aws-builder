package com.atex.shared;

import com.atex.ace.EnvironmentType;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.constructs.Node;

import static com.atex.ace.EnvironmentType.*;

public class AtexCloudRDSApp
{
    public static void main(final String[] args)
    {
        App app = new App();

        // Customer name is always just lowercase, so both 'Zawya' and 'ZAWYA' will be 'zawya'
        String customerName = getRequiredContext(app.getNode(), "customer", "Customer name (example: '-c customer=\"zawya\"') is a required input!").trim().toLowerCase();

        String accountId = getRequiredContext(app.getNode(), "account", "Account ID (example: '-c account=\"123456789\"') is a required input!");
        String region = getRequiredContext(app.getNode(), "region", "Region (example: '-c region=\"eu-west-1\"') is a required input!");

        EnvironmentType environmentType = getEnvironmentType(app.getNode());

        String vpcId = getRequiredContext(app.getNode(), "vpcId", "VPC ID (-c vpcId=XXXXXX) is a required input!");

        String subnet1Id = getRequiredContext(app.getNode(), "subnet1Id", "Subnet 1 ID (-c subnet1Id=XXXXX) is a required input!");
        String subnet2Id = getRequiredContext(app.getNode(), "subnet2Id", "Subnet 2 ID (-c subnet2Id=XXXXX) is a required input!");

        RDSProperties rdsProperties = new RDSProperties(customerName, accountId, region, environmentType, vpcId, subnet1Id, subnet2Id);

        new AtexCloudRDSStack(app, "atex-cloud-rds", StackProps.builder()
                                                               .env(env(accountId, region))
//                                                               .env(Environment.builder()
//                                                                               .account("614675827434")
//                                                                               .region("eu-west-1")
//                                                                               .build())
                                                               .build(),
                              rdsProperties);

        app.synth();
    }

    private static Environment env(final String accountId,
                                   final String region)
    {
        return Environment.builder()
                          .account(accountId)
                          .region(region)
                          .build();
    }

    private static String getRequiredContext(final Node node,
                                             final String contextParameter,
                                             final String message)
    {
        String value = (String) node.tryGetContext(contextParameter);

        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(value.trim())) {
            throw new RuntimeException(message);
        }

        return value;
    }

    private static EnvironmentType getEnvironmentType(final Node node)
    {
        String envTypeString = (String) node.tryGetContext("env");

        if (StringUtils.isEmpty(envTypeString) || StringUtils.isEmpty(envTypeString.trim())) {
            throw new RuntimeException("Environment (dev|staging|prod, example: '-c env=\"staging\"') is a required parameter.");
        }

        EnvironmentType environmentType = fromString(envTypeString.toLowerCase());

        if (environmentType == null) {
            throw new RuntimeException(String.format("Environment value '%s' not one of the possible values (dev|staging|prod).", envTypeString));
        }

        return environmentType;
    }
}
