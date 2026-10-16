package com.labourse.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegisteredEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user.registered";

    public void publish(String userId, String role, String email) {
        kafkaTemplate.send(TOPIC, userId, new UserRegisteredEvent(userId, role, email));
    }

    public record UserRegisteredEvent(String userId, String role, String email) {}
}
