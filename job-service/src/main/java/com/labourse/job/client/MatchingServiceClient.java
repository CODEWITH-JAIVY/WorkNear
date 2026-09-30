package com.labourse.job.client;

import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.dto.NearbyLabourDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MatchingServiceClient {

    private final RestTemplate restTemplate;
    private static final String URL = "http://MATCHING-SERVICE/api/matching/find";

    public List<NearbyLabourDto> findNearbyLabour(Long jobId, JobPostRequest req) {
        Map<String, Object> body = Map.of(
                "jobId", String.valueOf(jobId),
                "latitude", req.getLatitude(),
                "longitude", req.getLongitude(),
                "requiredLabourType", req.getRequiredLabourType(),
                "skillsRequired", req.getSkillsRequired() == null ? List.of() : req.getSkillsRequired(),
                "minRating", req.getMinRating(),
                "radiusKm", req.getRadiusKm()
        );
        var response = restTemplate.exchange(URL, HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<List<NearbyLabourDto>>() {});
        return response.getBody();
    }
}
