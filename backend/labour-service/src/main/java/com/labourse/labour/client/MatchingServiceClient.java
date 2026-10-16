package com.labourse.labour.client;

import com.labourse.labour.entity.LabourProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

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

    // Pushes type/skills/subSkills/gender/languages/rate into matching-service's Redis cache —
    // without this, findMatches() has nothing to filter on for any of these fields.
    public void syncProfile(Long labourId, LabourProfile profile) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", profile.getLabourType());
        body.put("serviceCodesCsv", profile.getServiceCodesCsv());
        body.put("skillsCsv", profile.getSkillsCsv());
        body.put("subSkillsCsv", profile.getSubSkillsCsv());
        body.put("gender", profile.getGender() != null ? profile.getGender().name() : null);
        body.put("languagesCsv", profile.getLanguagesCsv());
        body.put("rate", profile.getRate());

        restTemplate.put(BASE + "/profile/{id}", body, labourId);
    }
}
