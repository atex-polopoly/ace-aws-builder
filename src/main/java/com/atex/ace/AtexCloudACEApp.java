package com.atex.ace;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.constructs.Node;

public class AtexCloudACEApp
{
    public static void main(final String[] args)
    {
        App app = new App();

        String customerName = getRequiredContext(app.getNode(), "customer-name", "Customer name ('customer-name') is a required input!");
        String databaseClusterARN = getRequiredContext(app.getNode(), "database-arn", "Database ARN ('database-arn') is a required input!");

        CommonProperties properties = new CommonProperties(customerName, databaseClusterARN);

        AtexCloudACECloudfrontStack cloudfrontStack =
            new AtexCloudACECloudfrontStack(app, "AceStagingInstallationCFApp",
                                            StackProps.builder()
                                                      .env(Environment.builder()
                                                                      .region("us-east-1")
                                                                      .build())
                                                      .crossRegionReferences(true) // this is not really necessary at the moment
                                                      .build(),
                                            properties);


        AtexCloudACEBaseStack baseStack =
            new AtexCloudACEBaseStack(app, "AceStagingInstallationApp",
                                      StackProps.builder()
                                                .env(Environment.builder()
                                                                .region("eu-west-1")
                                                                .build())
                                                .crossRegionReferences(true) // this is not really necessary at the moment
                                                .build(),
                                      properties);

        cloudfrontStack.addDependency(baseStack); // this is not really necessary at the moment

        app.synth();
    }

    private static String getRequiredContext(final Node node,
                                      final String contextParameter,
                                      final String message)
    {
        String value = (String) node.tryGetContext(contextParameter);

        if (value == null || value.trim().length() == 0) {
            throw new RuntimeException(message);
        }

        return value;
    }
}
