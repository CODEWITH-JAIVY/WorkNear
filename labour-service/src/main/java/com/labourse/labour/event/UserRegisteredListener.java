package com.labourse.labour.event;

import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.repository.LabourProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final LabourProfileRepository repository;

    @KafkaListener(topics = "user.registered", groupId = "labour-service")
    public void onUserRegistered(Map<String, Object> event) {
        String role = String.valueOf(event.get("role"));
        if (!"LABOUR".equals(role)) return;

        Long userId = Long.valueOf(String.valueOf(event.get("userId")));
        if (repository.findByUserId(userId).isPresent()) return;

        LabourProfile profile = new LabourProfile();
        profile.setUserId(userId);
        repository.save(profile);
        log.info("Created labour profile shell for userId={}", userId);
    }
}
