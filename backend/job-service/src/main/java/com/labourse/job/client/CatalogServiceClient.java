package com.labourse.job.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Same validate-before-accept pattern as labour-service's CatalogServiceClient — a job post's
// requiredLabourType must be a real catalog code before matching ever runs, otherwise it would
// silently never match any labour (their serviceCodes are catalog codes too).
@Component
@RequiredArgsConstructor
public class CatalogServiceClient {

    private final RestTemplate restTemplate;
    private static final String EXISTS_URL = "http://CATALOG-SERVICE/api/catalog/items/{code}/exists";

    public void validateCodeOrThrow(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("requiredLabourType is required");
        }
        Boolean exists = restTemplate.getForObject(EXISTS_URL, Boolean.class, Map.of("code", code));
        if (exists == null || !exists) {
            throw new IllegalArgumentException("Unknown service code: " + code);
        }
    }
}
