package com.crime.crimealertbackend;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;

public class S3Uploader {

    public static void main(String[] args) {

        String bucketName = "backendcrimealertbucketfordata";
        String localFolderPath = "D:/Courses/BTech/4th_sem/OOPs/lab/BackendCrimeAlert/Data";  // <-- Change to your folder path

        Region region = Region.AP_SOUTH_1;  // Your AWS region

        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create()) // Uses default AWS credentials
                .build()) {

            // Create bucket if it doesn't exist
            if (!bucketExists(s3, bucketName)) {
                createBucket(s3, bucketName, region);
            }

            File baseFolder = new File(localFolderPath);
            if (baseFolder.isDirectory()) {
                uploadDirectoryRecursively(s3, bucketName, baseFolder, baseFolder);
                System.out.println("✅ Entire folder uploaded successfully!");
            } else {
                System.out.println("⚠️ The specified path is not a folder.");
            }

        } catch (S3Exception e) {
            System.err.println("S3 Error: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean bucketExists(S3Client s3, String bucketName) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (S3Exception e) {
            return false;  // Either the bucket doesn't exist or you don't have permission.
        }
    }

    private static void createBucket(S3Client s3, String bucketName, Region region) {
        CreateBucketRequest request = CreateBucketRequest.builder()
                .bucket(bucketName)
                .createBucketConfiguration(CreateBucketConfiguration.builder()
                        .locationConstraint(region.id())
                        .build())
                .build();
        s3.createBucket(request);
        System.out.println("🪣 Bucket created: " + bucketName);
    }

    private static void uploadDirectoryRecursively(S3Client s3, String bucketName, File baseFolder, File current) {

        for (File file : current.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectoryRecursively(s3, bucketName, baseFolder, file);  // 🔁 recurse into subdir
            } else {
                String key = baseFolder.toPath().relativize(file.toPath()).toString().replace("\\", "/");  // S3 uses '/' for paths
                putFile(s3, bucketName, key, file.getAbsolutePath());
            }
        }
    }

    private static void putFile(S3Client s3, String bucketName, String key, String filePath) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)  // Key with folder structure
                .build();

        s3.putObject(putObjectRequest, Paths.get(filePath));
        System.out.println("📤 Uploaded: " + key);
    }
}
