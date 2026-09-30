package com.labourse.customer.event;

import com.labourse.customer.entity.CustomerProfile;
import com.labourse.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final CustomerProfileRepository repository;

    @KafkaListener(topics = "user.registered", groupId = "customer-service")
    public void onUserRegistered(Map<String, Object> event) {
        String role = String.valueOf(event.get("role"));
        if (!"CUSTOMER".equals(role)) return; // labour-service handles LABOUR events itself

        Long userId = Long.valueOf(String.valueOf(event.get("userId")));
        if (repository.findByUserId(userId).isPresent()) return;

        CustomerProfile profile = new CustomerProfile();
        profile.setUserId(userId);
        repository.save(profile);
        log.info("Created customer profile shell for userId={}", userId);
    }
}
