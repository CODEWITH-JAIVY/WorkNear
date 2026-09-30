package com.labourse.labour.service;

import com.labourse.labour.client.MatchingServiceClient;
import com.labourse.labour.dto.LabourProfileDto;
import com.labourse.labour.dto.LocationUpdateDto;
import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.repository.LabourProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabourProfileService {

    private final LabourProfileRepository repository;
    private final MatchingServiceClient matchingServiceClient;

    public LabourProfile getByUserId(Long userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user " + userId));
    }

    public LabourProfile update(Long userId, LabourProfileDto dto) {
        LabourProfile profile = getByUserId(userId);
        profile.setName(dto.getName());
        profile.setLabourType(dto.getLabourType());
        profile.setEmploymentType(dto.getEmploymentType());
        profile.setSkillsCsv(dto.getSkillsCsv());
        profile.setAbout(dto.getAbout());
        profile.setCity(dto.getCity());
        return repository.save(profile);
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
}
