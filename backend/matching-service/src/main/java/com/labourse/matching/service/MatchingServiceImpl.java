package com.labourse.matching.service;

import com.labourse.matching.dto.JobMatchRequest;
import com.labourse.matching.dto.LabourProfileSyncDto;
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

            // The core 100%-accuracy change: a labour now offers MANY catalog service-codes
            // (serviceCodes, a CSV) instead of one single "type" string. A job asks for exactly
            // ONE catalog code (job.requiredLabourType holds a ServiceItem.code, e.g.
            // "HOME_REPAIR__ELECTRICIAN") — matches if that one code is anywhere in the labour's
            // list. "type" is still synced but only as a display-only coarse category now.
            boolean typeMatches = csvContains(profile.get("serviceCodes"), job.getRequiredLabourType());
            boolean isAvailable = Boolean.parseBoolean(profile.get("available"));
            boolean isKycVerified = Boolean.parseBoolean(profile.get("kycVerified"));
            double rating = Double.parseDouble(profile.getOrDefault("rating", "0"));
            boolean ratingOk = job.getMinRating() == null || rating >= job.getMinRating();
            boolean skillOk = job.getSkillsRequired() == null || job.getSkillsRequired().isEmpty()
                    || hasSkillOverlap(job.getSkillsRequired(), profile.get("skills"));

            // Sub-skill, e.g. "KITCHEN_CLEANING" within the broader "CLEANING" type
            boolean subSkillOk = job.getSubSkill() == null || job.getSubSkill().isBlank()
                    || csvContains(profile.get("subSkills"), job.getSubSkill());

            // Gender is an optional CUSTOMER preference, not a hard platform rule — only applied
            // when the job explicitly asks for one. Most jobs (electrician, carpenter, etc.)
            // should never set this.
            boolean genderOk = job.getGenderPreference() == null || job.getGenderPreference().isBlank()
                    || job.getGenderPreference().equalsIgnoreCase(profile.get("gender"));

            boolean languageOk = job.getLanguagePreference() == null || job.getLanguagePreference().isBlank()
                    || csvContains(profile.get("languages"), job.getLanguagePreference());

            // If the customer set a budget, a labour with no rate on file is excluded rather than
            // assumed-affordable — safer default than silently matching someone who may cost more.
            boolean budgetOk = job.getMaxBudget() == null
                    || (profile.get("rate") != null && Double.parseDouble(profile.get("rate")) <= job.getMaxBudget());

            if (typeMatches && isAvailable && isKycVerified && ratingOk && skillOk
                    && subSkillOk && genderOk && languageOk && budgetOk) {
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

    @Override
    public void syncLabourProfile(String labourId, LabourProfileSyncDto dto) {
        Map<String, String> fields = new HashMap<>();
        if (dto.getType() != null) fields.put("type", dto.getType());
        if (dto.getServiceCodesCsv() != null) fields.put("serviceCodes", dto.getServiceCodesCsv());
        if (dto.getSkillsCsv() != null) fields.put("skills", dto.getSkillsCsv());
        if (dto.getSubSkillsCsv() != null) fields.put("subSkills", dto.getSubSkillsCsv());
        if (dto.getGender() != null) fields.put("gender", dto.getGender());
        if (dto.getLanguagesCsv() != null) fields.put("languages", dto.getLanguagesCsv());
        if (dto.getRate() != null) fields.put("rate", String.valueOf(dto.getRate()));
        if (!fields.isEmpty()) {
            redisTemplate.opsForHash().putAll(PROFILE_KEY_PREFIX + labourId, fields);
        }
    }

    private boolean hasSkillOverlap(List<String> required, String labourSkillsCsv) {
        if (labourSkillsCsv == null) return false;
        Set<String> labourSkills = Set.of(labourSkillsCsv.split(","));
        return required.stream().anyMatch(labourSkills::contains);
    }

    private boolean csvContains(String csv, String value) {
        if (csv == null) return false;
        return Set.of(csv.split(",")).contains(value);
    }

    private Map<String, String> decodeHash(Map<byte[], byte[]> raw) {
        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(new String(k), new String(v)));
        return result;
    }
}
