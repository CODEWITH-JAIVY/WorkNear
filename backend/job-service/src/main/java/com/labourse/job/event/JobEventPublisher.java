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

    // A labour marked interest — notification-service tells the customer "someone's interested",
    // and this is also the trigger for chat-service to let this (customer, labour) pair talk.
    public void publishJobInterestExpressed(Long jobId, Long customerId, Long labourId) {
        kafkaTemplate.send("job.interest.expressed", String.valueOf(jobId),
                Map.of("jobId", jobId, "customerId", customerId, "labourId", labourId));
    }

    // Customer picked one labour from the interested list. selectedLabourId gets the "you got
    // the job" push; everyone else in notInterestedLabourIds gets a polite "job was filled" push
    // so they're not left wondering — this list replaces the old single-labourId job.accepted event.
    public void publishJobAssigned(Long jobId, Long customerId, Long selectedLabourId,
                                    List<Long> notSelectedLabourIds) {
        kafkaTemplate.send("job.accepted", String.valueOf(jobId),
                Map.of("jobId", jobId, "customerId", customerId, "labourId", selectedLabourId,
                        "notSelectedLabourIds", notSelectedLabourIds));
    }

    // A contractor's bulk requirement went out to nearby matching workers — no payment/commission
    // involved, this is purely a notification fan-out (see BulkJobService).
    public void publishBulkJobPosted(Long bulkJobId, List<String> candidateLabourIds) {
        kafkaTemplate.send("bulk.job.posted", String.valueOf(bulkJobId),
                Map.of("bulkJobId", bulkJobId, "candidateLabourIds", candidateLabourIds));
    }

    // Tells media-service to delete this job's attachments right away — the "same day if the
    // work is completed" half of the retention rule (the other half, "within 2 days", is an S3
    // lifecycle rule in terraform/s3.tf that catches jobs which never reach COMPLETED).
    public void publishJobCompleted(Long jobId, List<String> mediaKeys) {
        kafkaTemplate.send("job.completed", String.valueOf(jobId),
                Map.of("jobId", jobId, "mediaKeys", mediaKeys));
    }
}
