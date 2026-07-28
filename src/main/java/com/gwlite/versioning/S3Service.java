package com.gwlite.versioning;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

@Service
public class S3Service {

    @Value("${aws.s3.bucket:gwlite-document-versions}")
    private String bucketName;

    @Value("${aws.region:us-east-1}")
    private String region;

    private S3Client s3Client;

    private S3Client client() {
        if (s3Client == null) {
            s3Client = S3Client.builder().region(Region.of(region)).build();
        }
        return s3Client;
    }

    public String uploadSnapshot(String key, String content) {
        client().putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8))
        );
        return key;
    }

    public String downloadSnapshot(String key) {
        try (var response = client().getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())) {
            return new String(response.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read snapshot from S3: " + key, e);
        }
    }
}
