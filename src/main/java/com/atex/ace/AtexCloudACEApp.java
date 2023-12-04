package com.atex.ace;

import com.atex.ace.stack.AtexCloudACEBaseStack;
import com.atex.ace.stack.AtexCloudACECloudfrontStack;
import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.constructs.Node;

import static com.atex.ace.EnvironmentType.*;

public class AtexCloudACEApp
{
    public static void main(final String[] args)
    {
        App app = new App();

        // Customer name is always just lowercase, so both 'Zawya' and 'ZAWYA' will be 'zawya'
        String customerName = getRequiredContext(app.getNode(), "customer", "Customer name (example: '-c customer=\"zawya\"') is a required input!").trim().toLowerCase();

        String accountId = getRequiredContext(app.getNode(), "account", "Account ID (example: '-c account=\"123456789\"') is a required input!");
        String region = getRequiredContext(app.getNode(), "region", "Region (example: '-c region=\"eu-west-1\"') is a required input!");

        EnvironmentType environmentType = getEnvironmentType(app.getNode());

        String loadBalancerDomain = getRequiredContext(app.getNode(), "elb", "ELB domain (example: '-c elb=\"atex-Route-XXXXXXXXXXXXX-YYYYYYYYYY.eu-west-1.elb.amazonaws.com\"') is a required input!");
        String rdsClusterId = getRequiredContext(app.getNode(), "rds-cluster-id", "RDS cluster ID (example: '-c rds-cluster-id=\"cluster-XXXXXXXXXXXXXXXXXXXXXXXXX\"') is a required input!");

        HostedZoneDetails hostedZoneDetails;

        if (environmentType == PROD) {
            // In prod mode we also require properties dnsZoneId and dnsZoneName

            String zoneId = getRequiredContext(app.getNode(), "zoneId", "Hosted zone ID (example: '-c zoneId=XXXXXXXXXXXX' is a required input in production mode!");
            String zoneName = getRequiredContext(app.getNode(), "zoneName", "Hosted zone name (example: '-c zoneName=dev.atexcloud.io' is a required input in production mode!");

            hostedZoneDetails = new HostedZoneDetails(zoneId, zoneName);
        } else {
            hostedZoneDetails = environmentType.getZoneDetails();
        }

        // TODO: validate that the customer name is a valid customer shorthand (only a-z perhaps?)
        // TODO: validate that the account ID seems valid
        // TODO: validate that the region seems valid

        // TODO: we would like to have a longer version of the customer name as well (like Unione Sarda, compared to short version unionesarda)
        // TODO: we would sometimes like to have a longer version name for env type (like production instead of prod)

        CommonProperties properties = new CommonProperties(customerName, accountId, region, environmentType, loadBalancerDomain, rdsClusterId, hostedZoneDetails);

        AtexCloudACEBaseStack baseStack =
            new AtexCloudACEBaseStack(app, String.format("atex-cloud-%s-%s-ace-base", customerName, environmentType.getName()),
                                      StackProps.builder()
                                                .env(env(accountId, region))
                                                .tags(standardTags(properties))
                                                .crossRegionReferences(true) // this is not really necessary at the moment
                                                .build(),
                                      properties);

        AtexCloudACECloudfrontStack cloudfrontStack =
            new AtexCloudACECloudfrontStack(app, String.format("atex-cloud-%s-%s-ace-cloudfront", customerName, environmentType.getName()),
                                            StackProps.builder()
                                                      .env(env(accountId, "us-east-1")) // this stack has to be in North Virginia for CloudFront
                                                      .tags(standardTags(properties))
                                                      .crossRegionReferences(true) // this is not really necessary at the moment
                                                      .build(),
                                            properties);

        cloudfrontStack.addDependency(baseStack); // this is not really necessary at the moment

        app.synth();
    }

    private static Map<String, String> standardTags(final CommonProperties properties)
    {
        return Map.of("customer", properties.customerName(),
                      "atexSoftware", "ACE",
                      "environment", properties.environmentType().getName(),
                      "lastUpdated", Instant.now().toString()); // not sure if lastUpdated is a great idea or a horrible one
    }

    private static Environment env(final String accountId,
                                   final String region)
    {
        return Environment.builder()
                          .account(accountId)
                          .region(region)
                          .build();
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
}
