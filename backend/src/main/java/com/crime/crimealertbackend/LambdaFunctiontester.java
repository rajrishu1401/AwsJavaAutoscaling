package com.crime.crimealertbackend;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class LambdaFunctiontester {

    public static void main(String[] args) {

        String functionName = "sns-otp-sender";  // your function name

        LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.AP_SOUTH_1) // your AWS region
                .build();

        try {
            // Example payload: this depends on your Lambda handler input type!
            String jsonPayload = "{ \"otp\": \"123456\", \"phoneNumber\": \"+917667250711\" }";

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);

            String responseJson = response.payload().asUtf8String();

            System.out.println("Response from Lambda: " + responseJson);
            System.out.println("Status Code: " + response.statusCode());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lambdaClient.close();
        }
    }
}
