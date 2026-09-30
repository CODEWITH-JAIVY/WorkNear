package com.labourse.matching.service;

import com.labourse.matching.dto.JobMatchRequest;
import com.labourse.matching.dto.NearbyLabourDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.GeoOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Redis GEO + pipelined HGETALL are mocked here since a real Redis integration test for this
// lives better as a Testcontainers IT (same pattern as job-service's JobAcceptanceConcurrencyIT) —
// this covers the filtering DECISION logic, which is the part most likely to have a bug.
@ExtendWith(MockitoExtension.class)
class MatchingServiceImplTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock GeoOperations<String, String> geoOperations;
    @Mock HashOperations<String, Object, Object> hashOperations;

    @InjectMocks MatchingServiceImpl matchingService;

    private JobMatchRequest baseRequest() {
        JobMatchRequest req = new JobMatchRequest();
        req.setLatitude(28.5);
        req.setLongitude(77.3);
        req.setRequiredLabourType("PLUMBER");
        req.setRadiusKm(5.0);
        return req;
    }

    @SuppressWarnings("unchecked")
    private void mockGeoResults(String... labourIds) {
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> results = new ArrayList<>();
        for (String id : labourIds) {
            results.add(new GeoResult<>(
                    new RedisGeoCommands.GeoLocation<>(id, new Point(77.3, 28.5)),
                    new Distance(1.0)));
        }
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(results);

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("labour:geo"), any(Circle.class), any())).thenReturn(geoResults);
    }

    @SuppressWarnings("unchecked")
    private void mockProfiles(Map<String, String>... profiles) {
        List<Object> pipelinedResults = new ArrayList<>();
        for (Map<String, String> p : profiles) {
            Map<byte[], byte[]> raw = new HashMap<>();
            p.forEach((k, v) -> raw.put(k.getBytes(), v.getBytes()));
            pipelinedResults.add(raw);
        }
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(pipelinedResults);
    }

    @Test
    void excludesUnverifiedKycLabour_evenIfAvailableAndNearby() {
        mockGeoResults("labour1");
        mockProfiles(Map.of(
                "type", "PLUMBER", "available", "true", "kycVerified", "false", "rating", "4.5"
        ));

        JobMatchRequest req = baseRequest();
        List<NearbyLabourDto> result = matchingService.findMatchingLabour(req);

        assertThat(result).isEmpty(); // the exact bug class the KYC integration was built to prevent
    }

    @Test
    void excludesUnavailableLabour_evenIfKycVerifiedAndTypeMatches() {
        mockGeoResults("labour2");
        mockProfiles(Map.of(
                "type", "PLUMBER", "available", "false", "kycVerified", "true", "rating", "4.0"
        ));

        List<NearbyLabourDto> result = matchingService.findMatchingLabour(baseRequest());

        assertThat(result).isEmpty();
    }

    @Test
    void excludesLabourBelowMinRatingThreshold() {
        mockGeoResults("labour3");
        mockProfiles(Map.of(
                "type", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "2.5"
        ));

        JobMatchRequest req = baseRequest();
        req.setMinRating(4.0);

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void includesLabour_whenAllCriteriaPass_sortedByDistanceThenRating() {
        mockGeoResults("nearLowRated", "sameDistHighRated");
        mockProfiles(
                Map.of("type", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "3.0"),
                Map.of("type", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "4.8")
        );

        List<NearbyLabourDto> result = matchingService.findMatchingLabour(baseRequest());

        assertThat(result).hasSize(2);
        // Same distance (mocked identically) → higher rating breaks the tie and comes first
        assertThat(result.get(0).getLabourId()).isEqualTo("sameDistHighRated");
    }

    @Test
    void returnsEmpty_whenNoOneIsWithinRadius() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("labour:geo"), any(Circle.class), any()))
                .thenReturn(new GeoResults<>(List.of()));

        assertThat(matchingService.findMatchingLabour(baseRequest())).isEmpty();
    }
}
