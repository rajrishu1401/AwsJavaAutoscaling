package com.crime.crimealertbackend;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

public class InternetGatewayManager {
    private final Ec2Client ec2Client;

    public InternetGatewayManager() {
        this.ec2Client = Ec2Client.builder().build();
    }

    // Method to check if an Internet Gateway with the given name exists
    private String findInternetGatewayByName(String gatewayName) {
        DescribeInternetGatewaysResponse response = ec2Client.describeInternetGateways();

        for (InternetGateway igw : response.internetGateways()) {
            for (Tag tag : igw.tags()) {
                if (tag.key().equals("Name") && tag.value().equals(gatewayName)) {
                    return igw.internetGatewayId();
                }
            }
        }
        return null;
    }

    // Method to check if the IGW is already attached to the VPC
    private boolean isInternetGatewayAttached(String igwId, String vpcId) {
        DescribeInternetGatewaysResponse response = ec2Client.describeInternetGateways(
                DescribeInternetGatewaysRequest.builder()
                        .internetGatewayIds(igwId)
                        .build());

        List<InternetGatewayAttachment> attachments = response.internetGateways().get(0).attachments();
        for (InternetGatewayAttachment attachment : attachments) {
            if (attachment.vpcId().equals(vpcId) && attachment.state().equals(AttachmentStatus.ATTACHED)) {
                return true;
            }
        }
        return false;
    }

    // Method to create an Internet Gateway
    public String createInternetGateway(String gatewayName) {
        String existingIgwId = findInternetGatewayByName(gatewayName);
        if (existingIgwId != null) {
            System.out.println("Internet Gateway with name '" + gatewayName + "' already exists: " + existingIgwId);
            return existingIgwId;
        }

        try {
            CreateInternetGatewayResponse igwResponse = ec2Client.createInternetGateway();
            String igwId = igwResponse.internetGateway().internetGatewayId();
            System.out.println("Internet Gateway Created: " + igwId);

            // Tag the Internet Gateway with a name
            ec2Client.createTags(CreateTagsRequest.builder()
                    .resources(igwId)
                    .tags(Tag.builder().key("Name").value(gatewayName).build())
                    .build());

            System.out.println("Internet Gateway Tagged: " + gatewayName);
            return igwId;
        } catch (Exception e) {
            System.err.println("Error creating Internet Gateway: " + e.getMessage());
            return null;
        }
    }

    // Method to attach Internet Gateway to VPC
    public void attachInternetGateway(String igwId, String vpcId) {
        if (igwId == null || vpcId == null) {
            System.err.println("Invalid IGW or VPC ID!");
            return;
        }

        if (isInternetGatewayAttached(igwId, vpcId)) {
            System.out.println("Internet Gateway " + igwId + " is already attached to VPC " + vpcId);
            return;
        }

        try {
            ec2Client.attachInternetGateway(AttachInternetGatewayRequest.builder()
                    .internetGatewayId(igwId)
                    .vpcId(vpcId)
                    .build());

            System.out.println("Internet Gateway " + igwId + " attached to VPC " + vpcId);
        } catch (Exception e) {
            System.err.println("Error attaching Internet Gateway: " + e.getMessage());
        }
    }

    /*
    public static void main(String[] args) {
        InternetGatewayManager igwManager = new InternetGatewayManager();

        // Step 1: Create or get existing Internet Gateway
        String igwId = igwManager.createInternetGateway("crimealertgateway");

        // Replace with your VPC ID (Should be created earlier)
        String vpcId = "vpc-05b4da64c5210d729";

        // Step 2: Attach Internet Gateway to VPC if not already attached
        igwManager.attachInternetGateway(igwId, vpcId);
    }
    */
}
