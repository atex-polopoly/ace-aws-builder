package com.atex.shared.configuration;

import com.atex.ace.EnvironmentType;

public record CognitoProperties(String customerName, String accountId, String region, EnvironmentType environmentType)
{
}
