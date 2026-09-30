package com.labourse.matching.service;

import com.labourse.matching.dto.JobMatchRequest;
import com.labourse.matching.dto.NearbyLabourDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String GEO_KEY = "labour:geo";
    private static final String PROFILE_KEY_PREFIX = "labour:profile:";

    @Override
    public List<NearbyLabourDto> findMatchingLabour(JobMatchRequest job) {
        Circle area = new Circle(
                new Point(job.getLongitude(), job.getLatitude()),
                new Distance(job.getRadiusKm(), Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                redisTemplate.opsForGeo().radius(GEO_KEY, area,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance().sortAscending().limit(200));

        if (geoResults == null || geoResults.getContent().isEmpty()) {
            return List.of();
        }

        List<String> candidateIds = geoResults.getContent().stream()
                .map(r -> r.getContent().getName()).toList();

        List<Object> profiles = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            candidateIds.forEach(id -> conn.hashCommands().hGetAll((PROFILE_KEY_PREFIX + id).getBytes()));
            return null;
        });

        List<NearbyLabourDto> matched = new ArrayList<>();
        for (int i = 0; i < candidateIds.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<byte[], byte[]> raw = (Map<byte[], byte[]>) profiles.get(i);
            if (raw == null || raw.isEmpty()) continue;

            Map<String, String> profile = decodeHash(raw);

            boolean typeMatches = job.getRequiredLabourType().equalsIgnoreCase(profile.get("type"));
            boolean isAvailable = Boolean.parseBoolean(profile.get("available"));
            boolean isKycVerified = Boolean.parseBoolean(profile.get("kycVerified"));
            double rating = Double.parseDouble(profile.getOrDefault("rating", "0"));
            boolean ratingOk = job.getMinRating() == null || rating >= job.getMinRating();
            boolean skillOk = job.getSkillsRequired() == null || job.getSkillsRequired().isEmpty()
                    || hasSkillOverlap(job.getSkillsRequired(), profile.get("skills"));

            if (typeMatches && isAvailable && isKycVerified && ratingOk && skillOk) {
                double distanceKm = geoResults.getContent().get(i).getDistance().getValue();
                matched.add(new NearbyLabourDto(candidateIds.get(i), distanceKm, rating));
            }
        }

        matched.sort(Comparator.comparingDouble(NearbyLabourDto::getDistanceKm)
                .thenComparing(Comparator.comparingDouble(NearbyLabourDto::getRating).reversed()));

        int limit = job.getMaxCandidates() != null ? job.getMaxCandidates() : 20;
        return matched.stream().limit(limit).toList();
    }

    @Override
    public void updateLabourLocation(String labourId, double lat, double lon) {
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(lon, lat), labourId);
    }

    @Override
    public void setLabourAvailability(String labourId, boolean available) {
        redisTemplate.opsForHash().put(PROFILE_KEY_PREFIX + labourId, "available", String.valueOf(available));
    }

    @Override
    public void setLabourKycVerified(String labourId, boolean verified) {
        redisTemplate.opsForHash().put(PROFILE_KEY_PREFIX + labourId, "kycVerified", String.valueOf(verified));
    }

    private boolean hasSkillOverlap(List<String> required, String labourSkillsCsv) {
        if (labourSkillsCsv == null) return false;
        Set<String> labourSkills = Set.of(labourSkillsCsv.split(","));
        return required.stream().anyMatch(labourSkills::contains);
    }

    private Map<String, String> decodeHash(Map<byte[], byte[]> raw) {
        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(new String(k), new String(v)));
        return result;
    }
}
