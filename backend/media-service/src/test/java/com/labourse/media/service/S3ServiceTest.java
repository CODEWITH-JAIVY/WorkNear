package com.labourse.media.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock S3Client s3Client;

    private S3Service service(String cdn) {
        S3Service s = new S3Service(s3Client);
        ReflectionTestUtils.setField(s, "bucket", "labourse-media-prod");
        ReflectionTestUtils.setField(s, "cdnBaseUrl", cdn);
        return s;
    }

    @Test
    void uploadWithKey_returnsBothUrlAndKey_underTheRequestedFolder() {
        MockMultipartFile file = new MockMultipartFile("file", "tap.jpg", "image/jpeg", "x".getBytes());

        S3Service.UploadResult result = service("").uploadWithKey(file, "job-attachments");

        assertThat(result.key()).startsWith("job-attachments/").endsWith("-tap.jpg");
        assertThat(result.url()).isEqualTo("https://labourse-media-prod.s3.amazonaws.com/" + result.key());

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo(result.key());
        assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void uploadWithKey_usesCdnUrlWhenConfigured_butKeyStaysTheRawS3Key() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes());

        S3Service.UploadResult result = service("https://cdn.worknear.in").uploadWithKey(file, "job-attachments");

        assertThat(result.url()).startsWith("https://cdn.worknear.in/job-attachments/");
        assertThat(result.key()).doesNotContain("cdn.worknear.in"); // deletion needs the S3 key, not the CDN URL
    }

    @Test
    void uploadWithKey_generatesDistinctKeysForTheSameFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "same.jpg", "image/jpeg", "x".getBytes());
        S3Service s = service("");

        assertThat(s.uploadWithKey(file, "job-attachments").key())
                .isNotEqualTo(s.uploadWithKey(file, "job-attachments").key());
    }

    @Test
    void deleteJobAttachments_deletesOnlyKeysUnderTheJobAttachmentsPrefix() {
        int sent = service("").deleteJobAttachments(List.of(
                "job-attachments/a.jpg",
                "kyc-docs/aadhaar.jpg",          // must never be deleted by a job event
                "profile-photos/me.jpg",
                "job-attachments/../kyc-docs/x", // traversal-style key
                "job-attachments/b.mp4"));

        assertThat(sent).isEqualTo(2);

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        assertThat(captor.getValue().delete().objects()).extracting(ObjectIdentifier::key)
                .containsExactly("job-attachments/a.jpg", "job-attachments/b.mp4");
    }

    @Test
    void deleteJobAttachments_makesNoS3Call_whenNothingIsSafeToDelete() {
        int sent = service("").deleteJobAttachments(List.of("kyc-docs/x.jpg"));

        assertThat(sent).isZero();
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }
}
