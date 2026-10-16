package com.labourse.kyc.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KycEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishKycFullyVerified(Long labourId) {
        kafkaTemplate.send("kyc.verified", String.valueOf(labourId), Map.of("labourId", labourId));
    }
}
