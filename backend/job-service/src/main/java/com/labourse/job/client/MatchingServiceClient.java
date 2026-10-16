package com.labourse.job.client;

import com.labourse.job.dto.BulkJobPostRequest;
import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.dto.NearbyLabourDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MatchingServiceClient {

    private final RestTemplate restTemplate;
    private static final String URL = "http://MATCHING-SERVICE/api/matching/find";

    public List<NearbyLabourDto> findNearbyLabour(Long jobId, JobPostRequest req) {
        // HashMap, not Map.of() — several of these (minRating, genderPreference, budget) are
        // routinely null, and Map.of() throws NullPointerException on any null value.
        Map<String, Object> body = new HashMap<>();
        body.put("jobId", String.valueOf(jobId));
        body.put("latitude", req.getLatitude());
        body.put("longitude", req.getLongitude());
        body.put("requiredLabourType", req.getRequiredLabourType());
        body.put("skillsRequired", req.getSkillsRequired() == null ? List.of() : req.getSkillsRequired());
        body.put("subSkill", req.getSubSkill());
        body.put("genderPreference", req.getGenderPreference());
        body.put("languagePreference", req.getLanguagePreference());
        body.put("minRating", req.getMinRating());
        body.put("maxBudget", req.getBudget());
        body.put("radiusKm", req.getRadiusKm());
        var response = restTemplate.exchange(URL, HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<List<NearbyLabourDto>>() {});
        return response.getBody();
    }

    // Bulk-hire requirements don't carry sub-skill/gender/budget filters (kept simple
    // deliberately — contractors want reach, not narrow targeting) — minRating/kycVerified
    // still apply via matching-service's own defaults.
    public List<NearbyLabourDto> findNearbyLabourForBulkJob(Long bulkJobId, BulkJobPostRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("jobId", String.valueOf(bulkJobId));
        body.put("latitude", req.getLatitude());
        body.put("longitude", req.getLongitude());
        body.put("requiredLabourType", req.getRequiredLabourType());
        body.put("radiusKm", req.getRadiusKm());
        // maxCandidates deliberately left at matching-service's default (20) — a contractor
        // needing 10+ workers benefits from a wider notified pool than a single-job customer does
        var response = restTemplate.exchange(URL, HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<List<NearbyLabourDto>>() {});
        return response.getBody();
    }
}
