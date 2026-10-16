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
        callInternal("ban", userId);
    }

    /** auth-service must expose PUT /internal/auth/{id}/unban for this to work. */
    public void unbanUser(Long userId) {
        callInternal("unban", userId);
    }

    private void callInternal(String action, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        restTemplate.exchange("http://AUTH-SERVICE/internal/auth/{id}/" + action,
                HttpMethod.PUT, new HttpEntity<>(headers), Void.class, userId);
    }
}
