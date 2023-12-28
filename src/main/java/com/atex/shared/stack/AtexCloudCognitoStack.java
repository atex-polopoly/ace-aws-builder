package com.atex.shared.stack;

import com.atex.shared.configuration.CognitoProperties;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.CfnUserPoolUser;
import software.amazon.awscdk.services.cognito.UserPool;
import software.constructs.Construct;

public class AtexCloudCognitoStack
    extends Stack
{
    public AtexCloudCognitoStack(final Construct scope,
                                 final String id,
                                 final StackProps props,
                                 final CognitoProperties cognitoProperties)
    {
        super(scope, id, props);

        // TODO: lots of user pool and client properties...

        UserPool aceUserPool = UserPool.Builder.create(this, "AtexCloudCognitoACEPool")
                                               .userPoolName(String.format("atex-cloud-%s-ace-users",
                                                                           cognitoProperties.customerName()))
                                               .build();

        CfnUserPoolUser.Builder.create(this, "AndreasACEUser")
                               .userPoolId(aceUserPool.getUserPoolId())
                               .username("anilsson@atex.com")
                               .build();

        UserPool deskUserPool = UserPool.Builder.create(this, "AtexCloudCognitoACEPool")
                                                .userPoolName(String.format("atex-cloud-%s-desk-users",
                                                                            cognitoProperties.customerName()))
                                                .build();

        asOutput("AtexCloudACEUserPoolIdOutput", aceUserPool.getUserPoolId());
        asOutput("AtexCloudDeskUserPoolIdOutput", deskUserPool.getUserPoolId());

        // TODO: should always add "ourselves" in some way...
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
