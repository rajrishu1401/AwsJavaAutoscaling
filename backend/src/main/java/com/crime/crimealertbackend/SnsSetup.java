package com.crime.crimealertbackend;

import software.amazon.awssdk.regions.Region;

public class SnsSetup {
    public static void main(String[] args) {
        Region region = Region.AP_SOUTH_1;
        String filePath = "../sns-lambda/target/sns-lambda-1.0-SNAPSHOT.jar";  // relative to backend module
        String roleName = "SnsLambdaExecutionRole";

        S3Service s3Service = new S3Service(region);
        IAMService iamService = new IAMService(region);

        String bucketName = s3Service.createBucketAndUpload(filePath);
        String roleArn = iamService.createLambdaExecutionRole(roleName);

        System.out.println("\n=== AWS Setup Completed ===");
        System.out.println("S3 Bucket: " + bucketName);
        System.out.println("IAM Role ARN: " + roleArn);

        s3Service.close();
        iamService.close();
    }
}
