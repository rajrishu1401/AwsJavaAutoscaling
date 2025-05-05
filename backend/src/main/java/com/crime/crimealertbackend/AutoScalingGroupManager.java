package com.crime.crimealertbackend;

import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.*;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ElasticLoadBalancingV2Exception;

import software.amazon.awssdk.services.autoscaling.model.PredefinedMetricSpecification;




public class AutoScalingGroupManager {

    public static boolean isAutoScalingGroupExists(String asgName) {
        try (AutoScalingClient autoScaling = AutoScalingClient.create()) {
            DescribeAutoScalingGroupsRequest describeRequest = DescribeAutoScalingGroupsRequest.builder()
                    .autoScalingGroupNames(asgName)
                    .build();
            DescribeAutoScalingGroupsResponse response = autoScaling.describeAutoScalingGroups(describeRequest);
            return !response.autoScalingGroups().isEmpty();
        } catch (AutoScalingException e) {
            System.err.println("Error checking Auto Scaling Group existence: " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    public static void createAutoScalingGroup(String vpcZoneIdentifier, String targetGroupArn) {
        String asgName = "crimealert-asg-ec2";
        String launchTemplateName = "crimealert-lt-ec2";
        int healthCheckGracePeriod =200 ;

        try (AutoScalingClient autoScaling = AutoScalingClient.create()) {
            if (isAutoScalingGroupExists(asgName)) {
                System.out.println("Auto Scaling Group " + asgName + " already exists.");
                return;
            }

            // Create Auto Scaling Group
            CreateAutoScalingGroupRequest createRequest = CreateAutoScalingGroupRequest.builder()
                    .autoScalingGroupName(asgName)
                    .launchTemplate(LaunchTemplateSpecification.builder()
                            .launchTemplateName(launchTemplateName)
                            .build())
                    .vpcZoneIdentifier(vpcZoneIdentifier)
                    .minSize(1)
                    .maxSize(3)
                    .desiredCapacity(1)
                    .healthCheckType("ELB")
                    .healthCheckGracePeriod(healthCheckGracePeriod)
                    .targetGroupARNs(targetGroupArn)
                    .build();

            autoScaling.createAutoScalingGroup(createRequest);
            System.out.println("Auto Scaling Group created: " + asgName);

            // Add Target Tracking Scaling Policy (ALB request count per target)
            PutScalingPolicyRequest scalingPolicyRequest = PutScalingPolicyRequest.builder()
                    .autoScalingGroupName(asgName)
                    .policyName("RequestCountScalingPolicy")
                    .policyType("TargetTrackingScaling")
                    .targetTrackingConfiguration(TargetTrackingConfiguration.builder()
                            .predefinedMetricSpecification(PredefinedMetricSpecification.builder()
                                    .predefinedMetricType("ALBRequestCountPerTarget")
                                    .resourceLabel(getAlbResourceLabel(targetGroupArn))
                                    .build())
                            .targetValue(50.0)
                            .disableScaleIn(false)
                            .build())
                    .build();

            autoScaling.putScalingPolicy(scalingPolicyRequest);
            System.out.println("Scaling policy added: RequestCountScalingPolicy");

        } catch (AutoScalingException e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
        }
    }

    private static String getAlbResourceLabel(String targetGroupArn) {
        try (ElasticLoadBalancingV2Client elbv2 = ElasticLoadBalancingV2Client.create()) {
            // Get Target Group
            DescribeTargetGroupsResponse tgResponse = elbv2.describeTargetGroups(
                    DescribeTargetGroupsRequest.builder()
                            .targetGroupArns(targetGroupArn)
                            .build()
            );

            TargetGroup targetGroup = tgResponse.targetGroups().get(0);
            String targetGroupName = targetGroup.targetGroupName();
            String targetGroupId = extractIdFromArn(targetGroup.targetGroupArn());

            // Get Load Balancer ARN from Target Group
            String lbArn = targetGroup.loadBalancerArns().get(0);
            DescribeLoadBalancersResponse lbResponse = elbv2.describeLoadBalancers(
                    DescribeLoadBalancersRequest.builder()
                            .loadBalancerArns(lbArn)
                            .build()
            );

            LoadBalancer loadBalancer = lbResponse.loadBalancers().get(0);
            String lbName = loadBalancer.loadBalancerName();
            String lbId = extractIdFromArn(loadBalancer.loadBalancerArn());

            // Build and return resource label
            return String.format("app/%s/%s/targetgroup/%s/%s", lbName, lbId, targetGroupName, targetGroupId);

        } catch (ElasticLoadBalancingV2Exception e) {
            System.err.println("Error generating ALB resource label: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    private static String extractIdFromArn(String arn) {
        String[] parts = arn.split("/");
        return parts[parts.length - 1];
    }

    // Uncomment to run this directly
    /*
    public static void main(String[] args) {
        String vpcZoneIdentifier = "subnet-0bed78cc39ec5b9db,subnet-015c87a634e32086c";
        String targetGroupArn = "arn:aws:elasticloadbalancing:ap-south-1:961341512342:targetgroup/crimealert-tg-ec2/9f85ac16d98bceec";
        createAutoScalingGroup(vpcZoneIdentifier, targetGroupArn);
    }
    */
}
