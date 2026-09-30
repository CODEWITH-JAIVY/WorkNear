package com.labourse.admin.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {
    private final RestTemplate restTemplate;

    @Value("${internal.service.secret}")
    private String internalSecret;

    public void banUser(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        restTemplate.exchange("http://AUTH-SERVICE/internal/auth/{id}/ban",
                HttpMethod.PUT, new HttpEntity<>(headers), Void.class, userId);
    }
}
