package com.labourse.matching.controller;

import com.labourse.matching.dto.JobMatchRequest;
import com.labourse.matching.dto.LabourProfileSyncDto;
import com.labourse.matching.dto.NearbyLabourDto;
import com.labourse.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/find")
    public List<NearbyLabourDto> findMatches(@RequestBody JobMatchRequest request) {
        return matchingService.findMatchingLabour(request);
    }

    @PostMapping("/location/{labourId}")
    public void updateLocation(@PathVariable String labourId,
                                @RequestParam double lat, @RequestParam double lon) {
        matchingService.updateLabourLocation(labourId, lat, lon);
    }

    @PostMapping("/availability/{labourId}")
    public void setAvailability(@PathVariable String labourId, @RequestParam boolean available) {
        matchingService.setLabourAvailability(labourId, available);
    }

    @PostMapping("/kyc-status/{labourId}")
    public void setKycStatus(@PathVariable String labourId, @RequestParam boolean verified) {
        matchingService.setLabourKycVerified(labourId, verified);
    }

    // Called by labour-service every time a profile is created/updated — keeps type, skills,
    // sub-skills, gender, languages and rate in sync with what findMatches() filters on.
    @PutMapping("/profile/{labourId}")
    public void syncProfile(@PathVariable String labourId, @RequestBody LabourProfileSyncDto dto) {
        matchingService.syncLabourProfile(labourId, dto);
    }
}
