package com.crime.crimealertbackend;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.model.LambdaException;


public class LambdaFunctionCreator {

    public static void main(String[] args) {

        String functionName = "sns-otp-sender";
        String s3Bucket = "sns-lambda-c2b4c3de-53bb-4564-ab5d-feb745674cde";
        String s3Key = "sns-lambda-1.0-SNAPSHOT.jar";
        String lambdaRoleArn = "arn:aws:iam::961341512342:role/SnsLambdaExecutionRole";

        LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.AP_SOUTH_1)  // Replace with your region
                .build();

        try {
            FunctionCode code = FunctionCode.builder()
                    .s3Bucket(s3Bucket)
                    .s3Key(s3Key)
                    .build();

            CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .functionName(functionName)
                    .runtime(Runtime.JAVA17)  // since you're using Java 17
                    .role(lambdaRoleArn)
                    .handler("com.crime.crimealertbackend.SnsOtpSenderLambda::handleRequest")
                    .code(code)
                    .description("Lambda to send OTP via SNS")
                    .timeout(30)   // 30 seconds timeout
                    .memorySize(512) // MB
                    .build();

            CreateFunctionResponse response = lambdaClient.createFunction(request);
            System.out.println("✅ Lambda created! ARN: " + response.functionArn());

        } catch (LambdaException e) {
            System.err.println("❌ AWS Error: " + e.awsErrorDetails().errorMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ General Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lambdaClient.close();
        }
    }
}
