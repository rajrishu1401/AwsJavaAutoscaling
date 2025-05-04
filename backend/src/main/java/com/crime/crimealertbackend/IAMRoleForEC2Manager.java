package com.crime.crimealertbackend;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

public class IAMRoleForEC2Manager {

    public static void main(String[] args) {
        Region region = Region.AP_SOUTH_1;
        IamClient iam = IamClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        String roleName = "EC2_S3_Lambda_SNS_Role";

        // Create the IAM role and inline policy, and then call a post-creation function
        createIamRoleWithInlinePolicy(iam, roleName, () -> {
            // Post role creation logic: You can call another function here
            System.out.println("Role and policy created successfully. Now performing further actions.");
            // For example, create an instance profile for EC2
            createInstanceProfileAndAttachRole(iam, roleName);
        });

        iam.close();
    }

    // Create IAM Role and attach the inline policy
    public static void createIamRoleWithInlinePolicy(IamClient iam, String roleName, Runnable postRoleCreationAction) {
        // 1. Trust policy for EC2
        String assumeRolePolicyDocument = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": {\"Service\": \"ec2.amazonaws.com\"},\n" +
                "      \"Action\": \"sts:AssumeRole\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // 2. Create the IAM role
        try {
            CreateRoleRequest roleRequest = CreateRoleRequest.builder()
                    .roleName(roleName)
                    .assumeRolePolicyDocument(assumeRolePolicyDocument)
                    .description("Allows EC2 to access S3, invoke Lambda, and publish to SNS.")
                    .build();

            CreateRoleResponse roleResponse = iam.createRole(roleRequest);
            System.out.println("Role created successfully: " + roleResponse.role().arn());

            // If role creation is successful, execute the post-role-creation logic
            if (postRoleCreationAction != null) {
                postRoleCreationAction.run();
            }

        } catch (EntityAlreadyExistsException e) {
            System.out.println("Role already exists: " + roleName);
        } catch (IamException e) {
            System.err.println("Failed to create role: " + e.awsErrorDetails().errorMessage());
            return;
        }

        // 3. Inline permissions policy
        String policyDocument = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": \"s3:*\",\n" +
                "      \"Resource\": [\n" +
                "        \"arn:aws:s3:::backendcrimealertbucketfordata\",\n" +
                "        \"arn:aws:s3:::backendcrimealertbucketfordata/*\"\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": \"lambda:*\",\n" +
                "      \"Resource\": \"*\"\n" +  // Or restrict to specific Lambda ARN(s)
                "    },\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": \"sns:*\",\n" +
                "      \"Resource\": \"*\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        // 4. Attach the inline policy
        try {
            PutRolePolicyRequest policyRequest = PutRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName("EC2AccessPolicy")
                    .policyDocument(policyDocument)
                    .build();

            iam.putRolePolicy(policyRequest);
            System.out.println("Inline policy attached successfully.");
        } catch (IamException e) {
            System.err.println("Failed to attach inline policy: " + e.awsErrorDetails().errorMessage());
        }
    }

    // Example of a post-role-creation action (e.g., creating an instance profile)
    private static void createInstanceProfileAndAttachRole(IamClient iam, String roleName) {
        String instanceProfileName = roleName + "_InstanceProfile";

        // Create the instance profile
        try {
            CreateInstanceProfileRequest createRequest = CreateInstanceProfileRequest.builder()
                    .instanceProfileName(instanceProfileName)
                    .build();

            CreateInstanceProfileResponse response = iam.createInstanceProfile(createRequest);
            System.out.println("Instance profile created: " + response.instanceProfile().arn());

        } catch (EntityAlreadyExistsException e) {
            System.out.println("Instance profile already exists: " + instanceProfileName);
        } catch (IamException e) {
            System.err.println("Failed to create instance profile: " + e.awsErrorDetails().errorMessage());
            return;
        }

        // Add role to instance profile
        try {
            AddRoleToInstanceProfileRequest addRoleRequest = AddRoleToInstanceProfileRequest.builder()
                    .instanceProfileName(instanceProfileName)
                    .roleName(roleName)
                    .build();

            iam.addRoleToInstanceProfile(addRoleRequest);
            System.out.println("Role added to instance profile.");

        } catch (EntityAlreadyExistsException e) {
            System.out.println("Role already associated with instance profile.");
        } catch (IamException e) {
            System.err.println("Failed to add role to instance profile: " + e.awsErrorDetails().errorMessage());
        }
    }

}
