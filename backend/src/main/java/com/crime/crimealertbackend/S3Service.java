package com.crime.crimealertbackend;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.nio.file.Paths;
import java.util.UUID;

public class S3Service {
    private final S3Client s3;

    public S3Service(Region region) {
        this.s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String createBucketAndUpload(String filePath) {
        String bucketName = "sns-lambda-" + UUID.randomUUID();
        s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key("sns-lambda-1.0-SNAPSHOT.jar")
                        .build(),
                RequestBody.fromFile(Paths.get(filePath)));

        System.out.println("Created bucket and uploaded file: " + bucketName);
        return bucketName;
    }

    public void close() {
        s3.close();
    }
}
