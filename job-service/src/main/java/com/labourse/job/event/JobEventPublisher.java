package com.labourse.job.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishJobPosted(Long jobId, List<String> candidateLabourIds) {
        kafkaTemplate.send("job.posted", String.valueOf(jobId),
                Map.of("jobId", jobId, "candidateLabourIds", candidateLabourIds));
    }

    public void publishJobAccepted(Long jobId, Long customerId, Long labourId) {
        kafkaTemplate.send("job.accepted", String.valueOf(jobId),
                Map.of("jobId", jobId, "customerId", customerId, "labourId", labourId));
    }
}
