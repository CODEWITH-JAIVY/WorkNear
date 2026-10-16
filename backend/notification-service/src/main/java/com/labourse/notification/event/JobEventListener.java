package com.labourse.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labourse.notification.websocket.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventListener {

    private final PushService pushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Job posted → notify every matched candidate labour so they can accept
    @KafkaListener(topics = "job.posted", groupId = "notification-service")
    public void onJobPosted(Map<String, Object> event) throws Exception {
        Object jobId = event.get("jobId");
        @SuppressWarnings("unchecked")
        List<String> candidateIds = (List<String>) event.get("candidateLabourIds");
        if (candidateIds == null) return;

        String payload = objectMapper.writeValueAsString(Map.of(
                "type", "NEW_JOB_AVAILABLE", "jobId", jobId));

        candidateIds.forEach(labourId -> pushService.pushToUser(labourId, payload));
    }

    // A labour expressed interest → let the customer know someone's available to chat about it
    @KafkaListener(topics = "job.interest.expressed", groupId = "notification-service")
    public void onJobInterestExpressed(Map<String, Object> event) throws Exception {
        Object customerId = event.get("customerId");
        String payload = objectMapper.writeValueAsString(Map.of(
                "type", "NEW_INTEREST", "jobId", event.get("jobId"), "labourId", event.get("labourId")));

        pushService.pushToUser(String.valueOf(customerId), payload);
    }

    // Customer selected a labour → the winner gets "you got the job", everyone else who was
    // interested gets a polite "this one's filled" so they're not left wondering
    @KafkaListener(topics = "job.accepted", groupId = "notification-service")
    public void onJobAssigned(Map<String, Object> event) throws Exception {
        Object jobId = event.get("jobId");
        Object selectedLabourId = event.get("labourId");

        String wonPayload = objectMapper.writeValueAsString(Map.of(
                "type", "JOB_ASSIGNED_TO_YOU", "jobId", jobId));
        pushService.pushToUser(String.valueOf(selectedLabourId), wonPayload);

        @SuppressWarnings("unchecked")
        List<Object> notSelected = (List<Object>) event.get("notSelectedLabourIds");
        if (notSelected != null) {
            String filledPayload = objectMapper.writeValueAsString(Map.of(
                    "type", "JOB_FILLED_BY_ANOTHER", "jobId", jobId));
            notSelected.forEach(id -> pushService.pushToUser(String.valueOf(id), filledPayload));
        }
    }

    // A contractor's bulk requirement went out — notify every nearby matched worker
    @KafkaListener(topics = "bulk.job.posted", groupId = "notification-service")
    public void onBulkJobPosted(Map<String, Object> event) throws Exception {
        Object bulkJobId = event.get("bulkJobId");
        @SuppressWarnings("unchecked")
        List<String> candidateIds = (List<String>) event.get("candidateLabourIds");
        if (candidateIds == null) return;

        String payload = objectMapper.writeValueAsString(Map.of(
                "type", "BULK_JOB_AVAILABLE", "bulkJobId", bulkJobId));

        candidateIds.forEach(labourId -> pushService.pushToUser(labourId, payload));
    }
}
