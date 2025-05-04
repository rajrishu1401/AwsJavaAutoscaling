package com.crime.crimealertbackend;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.*;

import java.util.Arrays;

public class LoadBalancerManager {
    private static final ElasticLoadBalancingV2Client elbClient = ElasticLoadBalancingV2Client.create();

    // Method to check if a Load Balancer exists by name
    private static String getExistingLoadBalancerArn(String lbName) {
        try {
            DescribeLoadBalancersRequest request = DescribeLoadBalancersRequest.builder()
                    .names(lbName)
                    .build();

            DescribeLoadBalancersResponse response = elbClient.describeLoadBalancers(request);

            if (!response.loadBalancers().isEmpty()) {
                String lbArn = response.loadBalancers().get(0).loadBalancerArn();
                System.out.println("Load Balancer already exists: " + lbName + " | ARN: " + lbArn);
                return lbArn;
            }
        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error describing Load Balancer: " + e.awsErrorDetails().errorMessage());
        }
        return null;
    }

    // Method to check if a Listener exists for a given Load Balancer ARN
    private static boolean isListenerExists(String lbArn) {
        try {
            DescribeListenersRequest request = DescribeListenersRequest.builder()
                    .loadBalancerArn(lbArn)
                    .build();

            DescribeListenersResponse response = elbClient.describeListeners(request);

            if (!response.listeners().isEmpty()) {
                System.out.println("Listener already exists for Load Balancer: " + lbArn);
                return true;
            }
        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error describing Listener: " + e.awsErrorDetails().errorMessage());
        }
        return false;
    }

    // Method to create Load Balancer if it doesn't exist
    public static String createLoadBalancer(String vpcId, String subnet1, String subnet2, String securityGroupId) {
        String lbName = "crimealert-lb-ec2-with-asg";

        // Check if the Load Balancer already exists
        String existingLbArn = getExistingLoadBalancerArn(lbName);
        if (existingLbArn != null) {
            return existingLbArn;
        }

        try {
            // Step 1: Create Load Balancer
            CreateLoadBalancerResponse lbResponse = elbClient.createLoadBalancer(CreateLoadBalancerRequest.builder()
                    .name(lbName)
                    .scheme(LoadBalancerSchemeEnum.INTERNET_FACING)
                    .type(LoadBalancerTypeEnum.APPLICATION)
                    .ipAddressType(IpAddressType.IPV4)
                    .securityGroups(securityGroupId)
                    .subnets(subnet1, subnet2)
                    .build());

            String lbArn = lbResponse.loadBalancers().get(0).loadBalancerArn();
            System.out.println("Load Balancer Created: " + lbArn);

            return lbArn;
        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    // Method to create Listener on the Load Balancer if it doesn't exist
    public static void createListener(String lbArn, String targetGroupArn) {
        // Check if the Listener already exists
        if (isListenerExists(lbArn)) {
            return;  // Listener already exists, no need to create
        }

        try {
            // Step 2: Create Listener on Port 80
            CreateListenerResponse listenerResponse = elbClient.createListener(CreateListenerRequest.builder()
                    .loadBalancerArn(lbArn)
                    .protocol(ProtocolEnum.HTTP)
                    .port(80)
                    .defaultActions(Action.builder()
                            .type(ActionTypeEnum.FORWARD)
                            .targetGroupArn(targetGroupArn)
                            .build())
                    .build());

            System.out.println("Listener Created: HTTP 80 forwarding to Target Group");

        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
        }
    }

    // Method to check if the Load Balancer is active
    public static boolean isLoadBalancerActive(String lbArn) {
        try {
            DescribeLoadBalancersRequest request = DescribeLoadBalancersRequest.builder()
                    .loadBalancerArns(lbArn)
                    .build();

            DescribeLoadBalancersResponse response = elbClient.describeLoadBalancers(request);

            if (!response.loadBalancers().isEmpty()) {
                String state = response.loadBalancers().get(0).state().codeAsString();
                System.out.println("Load Balancer State: " + state);
                return state.equalsIgnoreCase(LoadBalancerStateEnum.ACTIVE.toString());
            }
        } catch (Exception e) {
            System.err.println("Error checking Load Balancer state: " + e.getMessage());
        }
        return false;
    }

    /*public static void main(String[] args) {
        // Replace with actual values
        String vpcId = "vpc-05b4da64c5210d729";
        String subnet1 = "subnet-02a9938425a83f1d5";
        String subnet2 = "subnet-0debca590a67c0ad6";
        String securityGroupId = "sg-0210e627cee7e6e2d";
        String targetGroupArn = "arn:aws:elasticloadbalancing:ap-south-1:961341512342:targetgroup/crimealert-tg-ec2/3b1ee521d3595ab6";

        // Create Load Balancer
        String lbArn = createLoadBalancer(vpcId, subnet1, subnet2, securityGroupId);

        // Attach Listener
        if (lbArn != null) {
            createListener(lbArn, targetGroupArn);
        }
    }*/
}
