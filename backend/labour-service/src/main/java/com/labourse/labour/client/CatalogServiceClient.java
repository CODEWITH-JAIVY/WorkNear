package com.labourse.labour.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Validates service codes against catalog-service before a profile is allowed to save — this is
// the actual "100% accuracy" guarantee: a typo'd or made-up code can never silently sit on a
// profile and never match anything. Fetches the full code list once per call rather than N
// separate /exists calls — 336 items is small enough that this is cheap and simpler than caching.
@Component
@RequiredArgsConstructor
public class CatalogServiceClient {

    private final RestTemplate restTemplate;
    private static final String ITEMS_URL = "http://CATALOG-SERVICE/api/catalog/items";

    public void validateCodesOrThrow(List<String> codes) {
        if (codes == null || codes.isEmpty()) return;

        var response = restTemplate.exchange(ITEMS_URL, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, String>>>() {});

        Set<String> validCodes = response.getBody() == null ? Set.of() : response.getBody().stream()
                .map(item -> item.get("code"))
                .collect(Collectors.toSet());

        List<String> invalid = codes.stream().filter(c -> !validCodes.contains(c)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Unknown service code(s): " + String.join(", ", invalid));
        }
    }
}
