package com.crime.crimealertbackend;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.FileWriter;
import java.io.IOException;

public class KeyPairManager {
    public static String createKeyPair(String keyName) {
        try (Ec2Client ec2 = Ec2Client.builder()
                .region(Region.AP_SOUTH_1)
                .build()) {

            // Check if the key already exists
            DescribeKeyPairsRequest describeRequest = DescribeKeyPairsRequest.builder()
                    .keyNames(keyName)
                    .build();

            ec2.describeKeyPairs(describeRequest);
            System.out.println("Key pair already exists: " + keyName);
            return keyName;

        } catch (Ec2Exception e) {
            // If key doesn't exist, create it
            if (e.awsErrorDetails().errorCode().equals("InvalidKeyPair.NotFound")) {
                try (Ec2Client ec2 = Ec2Client.builder()
                        .region(Region.AP_SOUTH_1)
                        .build()) {

                    CreateKeyPairRequest createRequest = CreateKeyPairRequest.builder()
                            .keyName(keyName)
                            .build();

                    CreateKeyPairResponse response = ec2.createKeyPair(createRequest);

                    String privateKey = response.keyMaterial();
                    String fileName = keyName + ".pem";

                    // Save private key to a file (Windows-safe)
                    try (FileWriter writer = new FileWriter(fileName)) {
                        writer.write(privateKey);
                        System.out.println("Key Pair Created: " + response.keyName());
                        System.out.println("Private key saved to: " + fileName);
                    } catch (IOException ioException) {
                        System.err.println("Error writing key to file: " + ioException.getMessage());
                    }

                    return response.keyName();
                }
            } else {
                System.err.println("Unexpected error: " + e.getMessage());
                return null;
            }
        }
    }
}
