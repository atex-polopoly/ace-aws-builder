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

        String accountId = getRequiredContext(app.getNode(), "account-id", "Account ID ('account-id') is a required input!");
        String region = getRequiredContext(app.getNode(), "region", "Region ('region') is a required input!");

        // Customer name is always just lowercase, so both 'Zawya' and 'ZAWYA' will be 'zawya'
        String customerName = getRequiredContext(app.getNode(), "customer-name", "Customer name ('customer-name') is a required input!").trim().toLowerCase();

        String loadBalancerDomain = getRequiredContext(app.getNode(), "elb-domain", "Load balancer domain ('elb-domain') is a required input!");
        String databaseClusterARN = getRequiredContext(app.getNode(), "database-arn", "Database ARN ('database-arn') is a required input!");

        // TODO: validate that the customer name is a valid customer shorthand (only a-z perhaps?)
        // TODO: validate that the account ID seems valid
        // TODO: validate that the region seems valid

        CommonProperties properties = new CommonProperties(customerName, accountId, region, DEV, loadBalancerDomain, databaseClusterARN);

        AtexCloudACEBaseStack baseStack =
            new AtexCloudACEBaseStack(app, "ACEBaseStack",
                                      StackProps.builder()
                                                .env(env(accountId, region))
                                                .tags(standardTags(properties))
                                                .crossRegionReferences(true) // this is not really necessary at the moment
                                                .build(),
                                      properties);

        AtexCloudACECloudfrontStack cloudfrontStack =
            new AtexCloudACECloudfrontStack(app, "ACECloudfrontStack",
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
                      "lastUpdated", Instant.now().toString());
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

        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException(message);
        }

        return value;
    }
}
