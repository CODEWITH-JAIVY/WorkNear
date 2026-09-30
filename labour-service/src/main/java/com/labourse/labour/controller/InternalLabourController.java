package com.labourse.labour.controller;

import com.labourse.labour.client.MatchingServiceClient;
import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.repository.LabourProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// Internal, service-to-service only — not routed through the public API gateway.
// In real prod, lock this down with mTLS or a shared internal-service secret header.
@RestController
@RequestMapping("/internal/labour")
@RequiredArgsConstructor
public class InternalLabourController {

    private final LabourProfileRepository repository;
    private final MatchingServiceClient matchingServiceClient;

    @PutMapping("/{userId}/rating")
    public void updateRating(@PathVariable Long userId, @RequestParam double avg, @RequestParam long count) {
        LabourProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        profile.setRating(avg);
        profile.setRatingCount((int) count);
        repository.save(profile);
    }

    @PutMapping("/{userId}/kyc-verified")
    public void markKycVerified(@PathVariable Long userId) {
        LabourProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        profile.setKycVerified(true);
        repository.save(profile);
        // Push into matching-service's Redis hash too — matching filters on this so an
        // unverified labour never gets dispatched a job even if they're online and nearby.
        matchingServiceClient.setKycVerified(userId, true);
    }
}

