package com.crime.crimealertbackend;

import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.*;

public class AutoScalingGroupManager {

    public static boolean isAutoScalingGroupExists(String asgName) {
        try (AutoScalingClient autoScaling = AutoScalingClient.create()) {
            // Step 1: Describe Auto Scaling Groups to check if it exists
            DescribeAutoScalingGroupsRequest describeRequest = DescribeAutoScalingGroupsRequest.builder()
                    .autoScalingGroupNames(asgName)
                    .build();

            DescribeAutoScalingGroupsResponse response = autoScaling.describeAutoScalingGroups(describeRequest);
            // If the group exists, the list will contain that group
            return !response.autoScalingGroups().isEmpty();
        } catch (AutoScalingException e) {
            System.err.println("Error checking Auto Scaling Group existence: " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    public static void createAutoScalingGroup(String vpcZoneIdentifier, String targetGroupArn) {
        String asgName = "crimealert-asg-ec2";
        String launchTemplateName = "crimealert-lt-ec2";
        int healthCheckGracePeriod = 300; // 5 minutes

        try (AutoScalingClient autoScaling = AutoScalingClient.create()) {
            // Check if Auto Scaling Group already exists
            if (isAutoScalingGroupExists(asgName)) {
                System.out.println("Auto Scaling Group " + asgName + " already exists.");
                return;
            }

            // Step 1: Create the Auto Scaling Group
            CreateAutoScalingGroupRequest request = CreateAutoScalingGroupRequest.builder()
                    .autoScalingGroupName(asgName)
                    .launchTemplate(LaunchTemplateSpecification.builder()
                            .launchTemplateName(launchTemplateName) // Use the latest version of the launch template
                            .build()
                    )
                    .vpcZoneIdentifier(vpcZoneIdentifier) // Subnets (comma-separated list)
                    .minSize(1)
                    .maxSize(3)
                    .desiredCapacity(2)
                    .healthCheckType("ELB")
                    .healthCheckGracePeriod(healthCheckGracePeriod)
                    .targetGroupARNs(targetGroupArn)
                    .build();

            autoScaling.createAutoScalingGroup(request);

            System.out.println("Auto Scaling Group Created: " + asgName);
        } catch (AutoScalingException e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
        }
    }

    /*public static void main(String[] args) {
        // Replace with your actual values
        String vpcZoneIdentifier = "subnet-0bed78cc39ec5b9db,subnet-015c87a634e32086c"; // Your subnets
        String targetGroupArn = "arn:aws:elasticloadbalancing:ap-south-1:961341512342:targetgroup/crimealert-tg-ec2/9f85ac16d98bceec"; // Your Target Group ARN

        createAutoScalingGroup(vpcZoneIdentifier, targetGroupArn);
    }*/
}
