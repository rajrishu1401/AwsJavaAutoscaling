package com.crime.crimealertbackend;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class App implements RequestHandler<Object, Object> {
    private final S3AsyncClient s3Client;

    public App() {
        s3Client = DependencyFactory.s3Client();
    }

    @Override
    public Object handleRequest(final Object input, final Context context) {
        return input;
    }

    public static void main(String[] args) {
        System.out.println("Crime Alert Application Started...");
        // Add logic here if you want to test AWS Lambda functionality locally
        App app = new App();
        Object response = app.handleRequest("Test Input", null);
        System.out.println("Response: " + response);
    }
}
