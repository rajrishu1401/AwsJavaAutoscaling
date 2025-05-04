package com.crime.crimealertbackend;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class VpcManager {
    private final Ec2Client ec2Client;

    public VpcManager() {
        this.ec2Client = Ec2Client.builder().build();
    }

    // Method to create a VPC or return existing one
    public String createVpc(String cidrBlock, String vpcName) {
        try {
            // Check if VPC already exists by name tag
            DescribeVpcsResponse describeResponse = ec2Client.describeVpcs(DescribeVpcsRequest.builder().build());
            for (Vpc vpc : describeResponse.vpcs()) {
                boolean cidrMatches = vpc.cidrBlock().equals(cidrBlock);
                boolean nameMatches = vpc.tags().stream()
                        .anyMatch(tag -> tag.key().equals("Name") && tag.value().equals(vpcName));
                if (cidrMatches && nameMatches) {
                    System.out.println("VPC already exists: " + vpc.vpcId());
                    return vpc.vpcId();
                }
            }

            // If not found, create new VPC
            CreateVpcRequest vpcRequest = CreateVpcRequest.builder()
                    .cidrBlock(cidrBlock)
                    .tagSpecifications(TagSpecification.builder()
                            .resourceType(ResourceType.VPC)
                            .tags(Tag.builder().key("Name").value(vpcName).build())
                            .build())
                    .build();

            CreateVpcResponse vpcResponse = ec2Client.createVpc(vpcRequest);
            System.out.println("VPC Created: " + vpcResponse.vpc().vpcId());
            return vpcResponse.vpc().vpcId();

        } catch (Ec2Exception e) {
            System.err.println("Error creating VPC: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }
}
