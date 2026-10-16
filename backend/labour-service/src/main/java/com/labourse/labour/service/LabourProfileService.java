package com.labourse.labour.service;

import com.labourse.labour.client.CatalogServiceClient;
import com.labourse.labour.client.MatchingServiceClient;
import com.labourse.labour.dto.LabourProfileDto;
import com.labourse.labour.dto.LocationUpdateDto;
import com.labourse.labour.dto.PublicLabourDto;
import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.repository.LabourProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabourProfileService {

    private final LabourProfileRepository repository;
    private final MatchingServiceClient matchingServiceClient;
    private final CatalogServiceClient catalogServiceClient;

    public LabourProfile getByUserId(Long userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user " + userId));
    }

    public LabourProfile update(Long userId, LabourProfileDto dto) {
        // Reject unknown/typo'd service codes before anything is saved — this is the actual
        // "100% accuracy" guarantee: a bad code can never silently sit on a profile unmatched.
        catalogServiceClient.validateCodesOrThrow(dto.getServiceCodes());

        LabourProfile profile = getByUserId(userId);
        profile.setName(dto.getName());
        profile.setLabourType(dto.getLabourType());
        profile.setEmploymentType(dto.getEmploymentType());
        profile.setServiceCodesCsv(dto.getServiceCodes() == null ? null : String.join(",", dto.getServiceCodes()));
        profile.setSkillsCsv(dto.getSkillsCsv());
        profile.setSubSkillsCsv(dto.getSubSkillsCsv());
        profile.setGender(dto.getGender());
        profile.setLanguagesCsv(dto.getLanguagesCsv());
        profile.setRate(dto.getRate());
        profile.setAbout(dto.getAbout());
        profile.setCity(dto.getCity());
        profile = repository.save(profile);

        // Previously only location/availability/KYC were synced — type, skills, and now the new
        // filters were never pushed to matching-service, so findMatches() could never actually
        // match on them. This call closes that gap.
        matchingServiceClient.syncProfile(userId, profile);

        return profile;
    }

    // Called frequently from the mobile app's background location updates
    public void updateLocation(Long userId, LocationUpdateDto dto) {
        LabourProfile profile = getByUserId(userId);
        profile.setCurrentLat(dto.getLat());
        profile.setCurrentLon(dto.getLon());
        repository.save(profile);
        matchingServiceClient.updateLocation(userId, dto.getLat(), dto.getLon());
    }

    public void setAvailability(Long userId, boolean available) {
        LabourProfile profile = getByUserId(userId);
        profile.setAvailableToday(available);
        repository.save(profile);
        matchingServiceClient.setAvailability(userId, available);
    }

    public PublicLabourDto getPublicProfile(Long userId) {
        return toPublic(getByUserId(userId));
    }

    static PublicLabourDto toPublic(LabourProfile p) {
        return new PublicLabourDto(
                p.getUserId(), p.getName(), p.getLabourType(),
                splitCsv(p.getServiceCodesCsv()),
                p.getRating(), p.getRatingCount(), p.isKycVerified(),
                p.getCity(), splitCsv(p.getLanguagesCsv()), p.getRate(), p.getAbout());
    }

    private static java.util.List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
