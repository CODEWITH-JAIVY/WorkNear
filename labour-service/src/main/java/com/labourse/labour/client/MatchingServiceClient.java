package com.labourse.labour.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class MatchingServiceClient {

    private final RestTemplate restTemplate;
    private static final String BASE = "http://MATCHING-SERVICE/api/matching";

    public void updateLocation(Long labourId, double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE + "/location/{id}")
                .queryParam("lat", lat).queryParam("lon", lon)
                .buildAndExpand(labourId).toUriString();
        restTemplate.postForEntity(url, null, Void.class);
    }

    public void setAvailability(Long labourId, boolean available) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE + "/availability/{id}")
                .queryParam("available", available)
                .buildAndExpand(labourId).toUriString();
        restTemplate.postForEntity(url, null, Void.class);
    }

    public void setKycVerified(Long labourId, boolean verified) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE + "/kyc-status/{id}")
                .queryParam("verified", verified)
                .buildAndExpand(labourId).toUriString();
        restTemplate.postForEntity(url, null, Void.class);
    }
}
