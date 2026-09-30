package com.labourse.rating.client;

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

    public void updateCachedRating(Long labourId, double avgRating, long count) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        String url = "http://LABOUR-SERVICE/internal/labour/{id}/rating?avg={avg}&count={count}";
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(headers), Void.class,
                labourId, avgRating, count);
    }
}
