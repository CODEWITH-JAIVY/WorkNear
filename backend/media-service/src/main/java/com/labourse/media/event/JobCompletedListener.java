package com.labourse.media.event;

import com.labourse.media.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// "Same day if the work is completed" half of the attachment retention rule. Jobs that never
// reach COMPLETED are caught by the 2-day S3 lifecycle rule instead (terraform/s3.tf).
@Component
@RequiredArgsConstructor
@Slf4j
public class JobCompletedListener {

    private final S3Service s3Service;

    @KafkaListener(topics = "job.completed", groupId = "media-service")
    public void onJobCompleted(Map<String, Object> event) {
        Object raw = event.get("mediaKeys");
        if (!(raw instanceof List<?> list) || list.isEmpty()) return;

        List<String> keys = list.stream().map(String::valueOf).toList();
        int deleted = s3Service.deleteJobAttachments(keys);
        log.info("job.completed jobId={} -> deleted {} of {} attachment(s)", event.get("jobId"), deleted, keys.size());
    }
}
