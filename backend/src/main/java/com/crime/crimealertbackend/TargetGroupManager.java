package com.crime.crimealertbackend;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.*;

public class TargetGroupManager {
    private final ElasticLoadBalancingV2Client elbClient;

    public TargetGroupManager() {
        this.elbClient = ElasticLoadBalancingV2Client.create();
    }

    // Check if a Target Group exists by name and return its ARN if found
    private String getExistingTargetGroupArn(String targetGroupName) {
        try {
            DescribeTargetGroupsResponse response = elbClient.describeTargetGroups(
                    DescribeTargetGroupsRequest.builder()
                            .names(targetGroupName)
                            .build());

            if (!response.targetGroups().isEmpty()) {
                String arn = response.targetGroups().get(0).targetGroupArn();
                System.out.println("Target Group already exists: " + targetGroupName + " | ARN: " + arn);
                return arn;
            }
        } catch (TargetGroupNotFoundException e) {
            // Target group doesn't exist, so we continue to create it
        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error describing Target Group: " + e.awsErrorDetails().errorMessage());
        }
        return null;
    }

    // Method to create a Target Group
    // Method to create a Target Group
    public String createTargetGroup(String vpcId, String targetGroupName) {
        String existingArn = getExistingTargetGroupArn(targetGroupName);
        if (existingArn != null) {
            return existingArn;
        }

        try {
            CreateTargetGroupRequest request = CreateTargetGroupRequest.builder()
                    .name(targetGroupName)
                    .protocol(ProtocolEnum.HTTP)
                    .port(9090)
                    .vpcId(vpcId)
                    .ipAddressType("ipv4")
                    .protocolVersion("HTTP1")
                    .targetType(TargetTypeEnum.INSTANCE)
                    .healthCheckPath("/health")                // 👈 Add this line
                    .healthCheckProtocol(ProtocolEnum.HTTP)    // Optional but explicit
                    .healthCheckPort("traffic-port")           // Default: use target port (9090)
                    .healthCheckEnabled(true)
                    .matcher(Matcher.builder().httpCode("200").build())
                    .build();


            CreateTargetGroupResponse response = elbClient.createTargetGroup(request);
            String targetGroupArn = response.targetGroups().get(0).targetGroupArn();
            System.out.println("Target Group Created: " + targetGroupName + " | ARN: " + targetGroupArn);
            return targetGroupArn;
        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error creating Target Group: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }


    /*
    public static void main(String[] args) {
        TargetGroupManager manager = new TargetGroupManager();
        String vpcId = "vpc-05b4da64c5210d729";
        manager.createTargetGroup(vpcId, "crimealert-tg-ec2");
    }
    */
}
