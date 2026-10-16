package com.labourse.kyc.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class LabourServiceClient {
    private final RestTemplate restTemplate;

    @Value("${internal.service.secret}")
    private String internalSecret;

    public void markKycVerified(Long labourId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        restTemplate.exchange("http://LABOUR-SERVICE/internal/labour/{id}/kyc-verified",
                HttpMethod.PUT, new HttpEntity<>(headers), Void.class, labourId);
    }
}
