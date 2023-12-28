package com.atex.shared.configuration;

import com.atex.ace.EnvironmentType;

public record VPCProperties(String customerName, String accountId, String region, EnvironmentType environmentType, String vpcCIDR)
{
}
