package com.crime.crimealertbackend;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;

public class LaunchTemplateManager {

    // Method to check if a Launch Template already exists by name
    private static String getExistingLaunchTemplateId(String templateName) {
        try (Ec2Client ec2 = Ec2Client.create()) {
            DescribeLaunchTemplatesRequest request = DescribeLaunchTemplatesRequest.builder()
                    .launchTemplateNames(templateName)
                    .build();

            DescribeLaunchTemplatesResponse response = ec2.describeLaunchTemplates(request);

            if (!response.launchTemplates().isEmpty()) {
                String templateId = response.launchTemplates().get(0).launchTemplateId();
                System.out.println("Launch Template already exists: " + templateName + " | ID: " + templateId);
                return templateId;
            }
        } catch (Ec2Exception e) {
            System.err.println("Error describing Launch Template: " + e.awsErrorDetails().errorMessage());
        }
        return null;
    }

    public static String createLaunchTemplate(String securityGroupId, String keyPairName) {
        String templateName = "crimealert-lt-ec2";
        String amiId = "ami-0e35ddab05955cf57"; // Ubuntu 24.04 LTS
        String instanceType = "t2.micro";
        String volumeSize = "8"; // 8 GiB
        String volumeType = "gp2"; // General Purpose SSD

        // Updated User Data Script (Base64 Encoded)
        String userDataScript = """
            #!/bin/bash
            sudo apt update -y
            sudo apt install -y unzip curl openjdk-17-jdk
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            unzip awscliv2.zip
            sudo ./aws/install
            mkdir -p /home/ubuntu/logs
            aws s3 cp s3://backendcrimealertbucketfordata/BackendCrimeAlert-1.0-SNAPSHOT.jar /home/ubuntu/crime-alert-backend.jar
            chmod +x /home/ubuntu/crime-alert-backend.jar
            nohup java -Dspring.profiles.active=prod -jar /home/ubuntu/crime-alert-backend.jar > /home/ubuntu/logs/backend.log 2>&1 &
            """;

        // Check if the Launch Template already exists
        String existingTemplateId = getExistingLaunchTemplateId(templateName);
        if (existingTemplateId != null) {
            return existingTemplateId;
        }

        try (Ec2Client ec2 = Ec2Client.create()) {
            RequestLaunchTemplateData requestTemplateData = RequestLaunchTemplateData.builder()
                    .imageId(amiId)
                    .instanceType(instanceType)
                    .keyName(keyPairName)
                    .networkInterfaces(
                            LaunchTemplateInstanceNetworkInterfaceSpecificationRequest.builder()
                                    .deviceIndex(0)
                                    .associatePublicIpAddress(true)
                                    .groups(securityGroupId)
                                    .build()
                    )
                    .blockDeviceMappings(
                            LaunchTemplateBlockDeviceMappingRequest.builder()
                                    .deviceName("/dev/sda1")
                                    .ebs(LaunchTemplateEbsBlockDeviceRequest.builder()
                                            .volumeSize(Integer.parseInt(volumeSize))
                                            .volumeType(volumeType)
                                            .build())
                                    .build()
                    )
                    .iamInstanceProfile(LaunchTemplateIamInstanceProfileSpecificationRequest.builder()
                            .name("EC2_S3_Lambda_SNS_Role_InstanceProfile")
                            .build())
                    .userData(Base64.getEncoder().encodeToString(userDataScript.getBytes()))
                    .build();

            CreateLaunchTemplateResponse response = ec2.createLaunchTemplate(CreateLaunchTemplateRequest.builder()
                    .launchTemplateName(templateName)
                    .launchTemplateData(requestTemplateData)
                    .build());

            System.out.println("Launch Template Created: " + response.launchTemplate().launchTemplateId());
            return response.launchTemplate().launchTemplateId();
        } catch (Ec2Exception e) {
            System.err.println("Error: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }
}
