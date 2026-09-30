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

    // Job accepted → notify the customer that someone is on the way
    @KafkaListener(topics = "job.accepted", groupId = "notification-service")
    public void onJobAccepted(Map<String, Object> event) throws Exception {
        Object customerId = event.get("customerId");
        String payload = objectMapper.writeValueAsString(Map.of(
                "type", "JOB_ACCEPTED", "jobId", event.get("jobId"), "labourId", event.get("labourId")));

        pushService.pushToUser(String.valueOf(customerId), payload);
    }
}
