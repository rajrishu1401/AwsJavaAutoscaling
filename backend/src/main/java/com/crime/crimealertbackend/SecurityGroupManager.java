package com.crime.crimealertbackend;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class SecurityGroupManager {

    // Method to check if a security group exists by name
    private static String getExistingSecurityGroupId(String vpcId, String groupName) {
        try (Ec2Client ec2 = Ec2Client.create()) {
            DescribeSecurityGroupsRequest describeRequest = DescribeSecurityGroupsRequest.builder()
                    .filters(Filter.builder()
                                    .name("vpc-id")
                                    .values(vpcId)
                                    .build(),
                            Filter.builder()
                                    .name("group-name")
                                    .values(groupName)
                                    .build())
                    .build();

            DescribeSecurityGroupsResponse describeResponse = ec2.describeSecurityGroups(describeRequest);

            if (!describeResponse.securityGroups().isEmpty()) {
                String securityGroupId = describeResponse.securityGroups().get(0).groupId();
                System.out.println("Security Group already exists: " + groupName + " | ID: " + securityGroupId);
                return securityGroupId;
            }
        } catch (Ec2Exception e) {
            System.err.println("Error describing Security Group: " + e.awsErrorDetails().errorMessage());
        }
        return null;
    }

    // Method to create a Security Group for the Load Balancer
    public static String createSecurityGroup(String vpcId) {
        String groupName = "crimealert-sg-for-lb-http-req";
        String description = "Allow HTTP request";

        String existingSecurityGroupId = getExistingSecurityGroupId(vpcId, groupName);
        if (existingSecurityGroupId != null) {
            return existingSecurityGroupId;
        }

        try (Ec2Client ec2 = Ec2Client.create()) {

            // Step 1: Create Security Group
            CreateSecurityGroupResponse createResponse = ec2.createSecurityGroup(CreateSecurityGroupRequest.builder()
                    .groupName(groupName)
                    .description(description)
                    .vpcId(vpcId)
                    .build());

            String securityGroupId = createResponse.groupId();
            System.out.println("Security Group Created: " + securityGroupId);

            // Step 2: Add Inbound Rule (Allow HTTP from Anywhere)
            IpRange ipRange = IpRange.builder()
                    .cidrIp("0.0.0.0/0") // Allow from any IPv4 address
                    .build();

            IpPermission ipPermission = IpPermission.builder()
                    .ipProtocol("tcp") // TCP Protocol
                    .fromPort(80) // HTTP Port 80
                    .toPort(80)
                    .ipRanges(ipRange)
                    .build();

            AuthorizeSecurityGroupIngressRequest ingressRequest = AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(securityGroupId)
                    .ipPermissions(ipPermission)
                    .build();

            ec2.authorizeSecurityGroupIngress(ingressRequest);
            System.out.println("Inbound Rule Added: Allow HTTP (80) from Anywhere");

            return securityGroupId;
        } catch (Ec2Exception e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    // Method to create a Security Group for the Launch Template
    public static String createSecurityGroupForTemplate(String vpcId,String SecurityGroupOfLB) {
        String securityGroupName = "crimealert-sg-lt-ec2";

        String existingSecurityGroupId = getExistingSecurityGroupId(vpcId, securityGroupName);
        if (existingSecurityGroupId != null) {
            return existingSecurityGroupId;
        }

        try (Ec2Client ec2Client = Ec2Client.create()) {
            // Step 1: Create Security Group
            CreateSecurityGroupResponse sgResponse = ec2Client.createSecurityGroup(CreateSecurityGroupRequest.builder()
                    .groupName(securityGroupName)
                    .description("Allow ssh and http requests")
                    .vpcId(vpcId)
                    .build());

            String sgId = sgResponse.groupId();
            System.out.println("Security Group Created: " + sgId);


            // Step 3: Add Inbound Rule for SSH (Port 22)
            ec2Client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(sgId)
                    .ipPermissions(IpPermission.builder()
                            .ipProtocol("tcp")
                            .fromPort(22)
                            .toPort(22)
                            .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                            .build())
                    .build());

            System.out.println("SSH (22) Rule Added");

            // Step 4: Add Inbound Rule for Spring Boot App (Port 9090)
            ec2Client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(sgId)
                    .ipPermissions(IpPermission.builder()
                            .ipProtocol("tcp")
                            .fromPort(9090)
                            .toPort(9090)
                            .userIdGroupPairs(UserIdGroupPair.builder()
                                    .groupId(SecurityGroupOfLB)
                                    .build())
                            .build())
                    .build());


            System.out.println("Spring Boot (9090) Rule Added");

            return sgId;
        } catch (Ec2Exception e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }
}
