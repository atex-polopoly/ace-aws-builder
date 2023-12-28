package com.atex.shared;

import com.atex.ace.EnvironmentType;
import com.atex.shared.configuration.VPCProperties;
import com.atex.shared.stack.AtexCloudVPCStack;
import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.constructs.Node;

import static com.atex.ace.EnvironmentType.*;

public class AtexCloudVPCApp
{
    public static void main(final String[] args)
    {
        App app = new App();

        // Customer name is always just lowercase, so both 'Zawya' and 'ZAWYA' will be 'zawya'
        String customerName = getRequiredContext(app.getNode(), "customer", "Customer name (example: '-c customer=\"zawya\"') is a required input!").trim().toLowerCase();

        String accountId = getRequiredContext(app.getNode(), "account", "Account ID (example: '-c account=\"123456789\"') is a required input!");
        String region = getRequiredContext(app.getNode(), "region", "Region (example: '-c region=\"eu-west-1\"') is a required input!");

        EnvironmentType environmentType = getEnvironmentType(app.getNode());

        String vpcCIDR = getRequiredContext(app.getNode(), "vpcCIDR", "VPC CIDR (example: '-c vpcCIDR=\"X.X.X.X/X\"') is a required input!");

        VPCProperties vpcProperties = new VPCProperties(customerName, accountId, region, environmentType, vpcCIDR);

        new AtexCloudVPCStack(app, "atex-cloud-vpc", StackProps.builder()
                                                               .env(env(accountId, region))
                                                               .tags(standardTags(vpcProperties))
                                                               .build(),
                              vpcProperties);

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

    private static Map<String, String> standardTags(final VPCProperties properties)
    {
        return Map.of("customer", properties.customerName(),
                      "atexSoftware", "ACE/Desk",
                      "environment", properties.environmentType().getName(),
                      "lastUpdated", Instant.now().toString()); // not sure if lastUpdated is a great idea or a horrible one
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
