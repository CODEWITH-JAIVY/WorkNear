package com.labourse.matching.service;

import com.labourse.matching.dto.JobMatchRequest;
import com.labourse.matching.dto.LabourProfileSyncDto;
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
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "false", "rating", "4.5"
        ));

        JobMatchRequest req = baseRequest();
        List<NearbyLabourDto> result = matchingService.findMatchingLabour(req);

        assertThat(result).isEmpty(); // the exact bug class the KYC integration was built to prevent
    }

    @Test
    void excludesUnavailableLabour_evenIfKycVerifiedAndTypeMatches() {
        mockGeoResults("labour2");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "false", "kycVerified", "true", "rating", "4.0"
        ));

        List<NearbyLabourDto> result = matchingService.findMatchingLabour(baseRequest());

        assertThat(result).isEmpty();
    }

    @Test
    void excludesLabourBelowMinRatingThreshold() {
        mockGeoResults("labour3");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "2.5"
        ));

        JobMatchRequest req = baseRequest();
        req.setMinRating(4.0);

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void includesLabour_whenAllCriteriaPass_sortedByDistanceThenRating() {
        mockGeoResults("nearLowRated", "sameDistHighRated");
        mockProfiles(
                Map.of("serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "3.0"),
                Map.of("serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "4.8")
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

    @Test
    void excludesLabour_whenGenderPreferenceSetAndDoesNotMatch() {
        mockGeoResults("labour4");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true",
                "rating", "4.5", "gender", "MALE"
        ));

        JobMatchRequest req = baseRequest();
        req.setGenderPreference("FEMALE");

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void includesLabour_whenGenderPreferenceMatches() {
        mockGeoResults("labour5");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true",
                "rating", "4.5", "gender", "FEMALE"
        ));

        JobMatchRequest req = baseRequest();
        req.setGenderPreference("FEMALE");

        assertThat(matchingService.findMatchingLabour(req)).hasSize(1);
    }

    @Test
    void includesLabour_whenNoGenderPreferenceSet_regardlessOfLabourGender() {
        // No preference means every trade (electrician, carpenter, etc.) keeps working as before —
        // this guards against the filter accidentally becoming a hard default.
        mockGeoResults("labour6");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true",
                "rating", "4.5", "gender", "MALE"
        ));

        assertThat(matchingService.findMatchingLabour(baseRequest())).hasSize(1);
    }

    @Test
    void excludesLabour_whenSubSkillRequestedButNotOffered() {
        mockGeoResults("labour7");
        mockProfiles(Map.of(
                "serviceCodes", "CLEANING", "available", "true", "kycVerified", "true",
                "rating", "4.0", "subSkills", "BATHROOM_CLEANING,FULL_HOME"
        ));

        JobMatchRequest req = baseRequest();
        req.setRequiredLabourType("CLEANING");
        req.setSubSkill("KITCHEN_CLEANING");

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void excludesLabour_whenOverBudget() {
        mockGeoResults("labour8");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true",
                "rating", "4.0", "rate", "800"
        ));

        JobMatchRequest req = baseRequest();
        req.setMaxBudget(500.0);

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void excludesLabour_whenBudgetSetButLabourHasNoRateOnFile() {
        // No rate on file + a budget filter active = excluded, not assumed-affordable
        mockGeoResults("labour9");
        mockProfiles(Map.of(
                "serviceCodes", "PLUMBER", "available", "true", "kycVerified", "true", "rating", "4.0"
        ));

        JobMatchRequest req = baseRequest();
        req.setMaxBudget(500.0);

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void syncLabourProfile_writesOnlyNonNullFieldsToRedisHash() {
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
                mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        LabourProfileSyncDto dto = new LabourProfileSyncDto();
        dto.setType("ELECTRICIAN");
        dto.setServiceCodesCsv("HOME_REPAIR__ELECTRICIAN,HOME_REPAIR__FAN_INSTALLATION_REPAIR");
        dto.setGender("MALE");
        // skillsCsv, subSkillsCsv, languagesCsv, rate intentionally left null

        matchingService.syncLabourProfile("labour10", dto);

        verify(hashOps).putAll(eq("labour:profile:labour10"), argThat(map ->
                map.get("type").equals("ELECTRICIAN")
                && map.get("serviceCodes").equals("HOME_REPAIR__ELECTRICIAN,HOME_REPAIR__FAN_INSTALLATION_REPAIR")
                && map.get("gender").equals("MALE")
                && !map.containsKey("skills")
                && !map.containsKey("rate")
        ));
    }

    // ---------- Multi-skill catalog matching (the core 100%-accuracy change) ----------

    @Test
    void includesLabour_whenJobsRequiredCodeIsOneOfSeveralTheLabourOffers() {
        mockGeoResults("multiSkillLabour");
        mockProfiles(Map.of(
                "serviceCodes", "HOME_REPAIR__ELECTRICIAN,HOME_REPAIR__PLUMBER,CLEANING__KITCHEN_CLEANING",
                "available", "true", "kycVerified", "true", "rating", "4.2"
        ));

        JobMatchRequest req = baseRequest();
        req.setRequiredLabourType("HOME_REPAIR__PLUMBER"); // one of three codes this labour offers

        assertThat(matchingService.findMatchingLabour(req)).hasSize(1);
    }

    @Test
    void excludesLabour_whenRequiredCodeIsNotAmongTheLabourOfferedCodes() {
        mockGeoResults("mismatchedLabour");
        mockProfiles(Map.of(
                "serviceCodes", "HOME_REPAIR__ELECTRICIAN,HOME_REPAIR__PLUMBER",
                "available", "true", "kycVerified", "true", "rating", "4.2"
        ));

        JobMatchRequest req = baseRequest();
        req.setRequiredLabourType("CLEANING__KITCHEN_CLEANING"); // not in this labour's offered list

        assertThat(matchingService.findMatchingLabour(req)).isEmpty();
    }

    @Test
    void excludesLabour_whenServiceCodesMissingEntirely() {
        // Profile never synced (or sync predates the catalog migration) — must fail closed, not match everything
        mockGeoResults("noCatalogDataLabour");
        mockProfiles(Map.of(
                "available", "true", "kycVerified", "true", "rating", "4.2"
        ));

        assertThat(matchingService.findMatchingLabour(baseRequest())).isEmpty();
    }
}
