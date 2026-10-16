package com.labourse.media.event;

import com.labourse.media.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCompletedListenerTest {

    @Mock S3Service s3Service;
    @InjectMocks JobCompletedListener listener;

    @Test
    void onJobCompleted_deletesTheJobsAttachmentKeys() {
        listener.onJobCompleted(Map.of("jobId", 7, "mediaKeys", List.of("job-attachments/a.jpg", "job-attachments/b.m4a")));

        verify(s3Service).deleteJobAttachments(List.of("job-attachments/a.jpg", "job-attachments/b.m4a"));
    }

    @Test
    void onJobCompleted_ignoresEventsWithNoKeys() {
        listener.onJobCompleted(Map.of("jobId", 7));
        listener.onJobCompleted(Map.of("jobId", 8, "mediaKeys", List.of()));

        verifyNoInteractions(s3Service);
    }
}
