package com.crime.crimealertbackend;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class SnsOtpSenderLambda implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        String phoneNumber = (String) event.get("phoneNumber");
        String otp = (String) event.get("otp");

        if (phoneNumber == null || otp == null) {
            return "Invalid input! 'phoneNumber' and 'otp' fields are required.";
        }

        String message = "Your OTP is: " + otp;
        context.getLogger().log("Attempting to send OTP to: " + phoneNumber);

        try (SnsClient snsClient = SnsClient.create()) {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phoneNumber)
                    .build();

            PublishResponse response = snsClient.publish(request);
            context.getLogger().log("Message ID: " + response.messageId());
            return "OTP sent successfully!";
        } catch (Exception e) {
            context.getLogger().log("Error sending OTP: " + e.getMessage());
            return "Failed to send OTP!";
        }
    }
}
