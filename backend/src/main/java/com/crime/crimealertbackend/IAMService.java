package com.crime.crimealertbackend;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class IAMService {
    private final IamClient iam;

    public IAMService(Region region) {
        this.iam = IamClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String createLambdaExecutionRole(String roleName) {
        String trustPolicy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {
                    "Service": "lambda.amazonaws.com"
                  },
                  "Action": "sts:AssumeRole"
                }
              ]
            }
            """;

        CreateRoleResponse roleResponse = iam.createRole(CreateRoleRequest.builder()
                .roleName(roleName)
                .assumeRolePolicyDocument(trustPolicy)
                .build());

        iam.attachRolePolicy(AttachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn("arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole")
                .build());

        iam.attachRolePolicy(AttachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn("arn:aws:iam::aws:policy/AmazonSNSFullAccess")
                .build());

        System.out.println("Created IAM role and attached policies.");
        return roleResponse.role().arn();
    }

    public void close() {
        iam.close();
    }
}
