package com.atex.ace;

public record CommonProperties (String customerName, String accountId, String region, EnvironmentType environmentType, String loadBalancerDomain, String databaseClusterId, String eventBusArn)
{

}
