package com.labourse.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class S3Service {

    // Only this prefix is ever deleted by event. job.completed arrives over Kafka, so a malformed
    // or forged payload must not be able to delete KYC documents or profile photos.
    public static final String JOB_ATTACHMENTS_PREFIX = "job-attachments/";

    public record UploadResult(String url, String key) {}

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.cdn-base-url:}")
    private String cdnBaseUrl;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String upload(MultipartFile file, String folder) {
        return uploadWithKey(file, folder).url();
    }

    // Returns the key as well as the URL — callers that need to delete the object later
    // (job attachments) store the key, not the URL.
    public UploadResult uploadWithKey(MultipartFile file, String folder) {
        try {
            String key = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = cdnBaseUrl.isBlank()
                    ? String.format("https://%s.s3.amazonaws.com/%s", bucket, key)
                    : cdnBaseUrl + "/" + key;
            return new UploadResult(url, key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    // Deletes only keys under job-attachments/; anything else in the list is skipped.
    // Returns how many keys were actually sent for deletion.
    public int deleteJobAttachments(List<String> keys) {
        List<ObjectIdentifier> safe = keys.stream()
                .filter(k -> k != null && k.startsWith(JOB_ATTACHMENTS_PREFIX) && !k.contains(".."))
                .map(k -> ObjectIdentifier.builder().key(k).build())
                .toList();
        if (safe.isEmpty()) return 0;

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(safe).build())
                .build());
        return safe.size();
    }
}
