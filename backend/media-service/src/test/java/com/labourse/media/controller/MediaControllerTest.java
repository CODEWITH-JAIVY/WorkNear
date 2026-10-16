package com.labourse.media.controller;

import com.labourse.media.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock S3Service s3Service;
    @InjectMocks MediaController controller;

    private MockMultipartFile file(String type) {
        return new MockMultipartFile("file", "f.bin", type, "x".getBytes());
    }

    @Test
    void upload_rejectsAFolderOutsideTheAllowlist() {
        assertThatThrownBy(() -> controller.upload(file("image/jpeg"), "anything-i-like"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unknown upload folder");
        verifyNoInteractions(s3Service);
    }

    @Test
    void upload_rejectsPathTraversalStyleFolders() {
        assertThatThrownBy(() -> controller.upload(file("image/jpeg"), "../kyc-docs"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(s3Service);
    }

    @Test
    void upload_acceptsPhotoVideoAndVoiceNoteForJobAttachments() {
        when(s3Service.uploadWithKey(any(), eq("job-attachments")))
                .thenReturn(new S3Service.UploadResult("https://u", "job-attachments/k"));

        for (String type : new String[]{"image/jpeg", "video/mp4", "audio/mpeg"}) {
            Map<String, String> r = controller.upload(file(type), "job-attachments");
            assertThat(r).containsEntry("key", "job-attachments/k").containsEntry("url", "https://u");
        }
    }

    @Test
    void upload_rejectsNonMediaFilesForJobAttachments() {
        assertThatThrownBy(() -> controller.upload(file("application/pdf"), "job-attachments"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("photo, video or voice note");
        verifyNoInteractions(s3Service);
    }

    @Test
    void upload_doesNotApplyTheMediaTypeRuleToOtherFolders() {
        // kyc-docs legitimately takes PDFs — the media-type rule is job-attachments only
        when(s3Service.uploadWithKey(any(), eq("kyc-docs")))
                .thenReturn(new S3Service.UploadResult("https://u", "kyc-docs/k"));

        assertThat(controller.upload(file("application/pdf"), "kyc-docs")).containsKey("key");
    }
}
