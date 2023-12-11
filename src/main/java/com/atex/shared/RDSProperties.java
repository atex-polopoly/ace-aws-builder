package com.atex.shared;

import com.atex.ace.EnvironmentType;

public record RDSProperties(String customerName, String accountId, String region, EnvironmentType environmentType, String vpcId, String subnet1Id, String subnet2Id)
{
}
