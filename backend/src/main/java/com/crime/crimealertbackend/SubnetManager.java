package com.crime.crimealertbackend;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

public class SubnetManager {
    private final Ec2Client ec2;

    public SubnetManager() {
        this.ec2 = Ec2Client.create();
    }

    // Check if a subnet already exists with the same VPC, CIDR, AZ, and Name
    private String findExistingSubnet(String vpcId, String cidrBlock, String availabilityZone, String subnetName) {
        DescribeSubnetsResponse response = ec2.describeSubnets();

        for (Subnet subnet : response.subnets()) {
            if (subnet.vpcId().equals(vpcId) &&
                    subnet.cidrBlock().equals(cidrBlock) &&
                    subnet.availabilityZone().equals(availabilityZone)) {

                for (Tag tag : subnet.tags()) {
                    if (tag.key().equals("Name") && tag.value().equals(subnetName)) {
                        System.out.println("Found existing subnet: " + subnetName + " | Subnet ID: " + subnet.subnetId());
                        return subnet.subnetId();
                    }
                }
            }
        }
        return null;
    }

    public String createSubnet(String vpcId, String subnetCidr, String availabilityZone, String subnetName) {
        String existingSubnetId = findExistingSubnet(vpcId, subnetCidr, availabilityZone, subnetName);
        if (existingSubnetId != null) {
            return existingSubnetId;
        }

        try {
            CreateSubnetRequest subnetRequest = CreateSubnetRequest.builder()
                    .vpcId(vpcId)
                    .cidrBlock(subnetCidr)
                    .availabilityZone(availabilityZone)
                    .tagSpecifications(TagSpecification.builder()
                            .resourceType(ResourceType.SUBNET)
                            .tags(Tag.builder().key("Name").value(subnetName).build())
                            .build())
                    .build();

            CreateSubnetResponse subnetResponse = ec2.createSubnet(subnetRequest);
            String subnetId = subnetResponse.subnet().subnetId();
            System.out.println("Subnet Created: " + subnetName + " | Subnet ID: " + subnetId);
            return subnetId;
        } catch (Ec2Exception e) {
            System.err.println("Error creating subnet " + subnetName + ": " + e.getMessage());
            return null;
        }
    }

    /*
    public static void main(String[] args) {
        SubnetManager manager = new SubnetManager();
        String vpcId = "vpc-05b4da64c5210d729";  // Replace with your actual VPC ID

        // Create or find Subnet 1
        String subnet1Id = manager.createSubnet(vpcId, "12.0.1.0/24", "ap-south-1a", "crimealert-subnet-1a");

        // Create or find Subnet 2
        String subnet2Id = manager.createSubnet(vpcId, "12.0.3.0/24", "ap-south-1b", "crimealert-subnet-1b");
    }
    */
}
