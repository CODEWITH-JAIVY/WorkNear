package com.labourse.labour.controller;

import com.labourse.labour.dto.LabourProfileDto;
import com.labourse.labour.dto.LocationUpdateDto;
import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.service.LabourProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/labour")
@RequiredArgsConstructor
public class LabourController {

    private final LabourProfileService service;

    @GetMapping("/me")
    public LabourProfile getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        return service.getByUserId(userId);
    }

    @PutMapping("/me")
    public LabourProfile updateMyProfile(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody LabourProfileDto dto) {
        return service.update(userId, dto);
    }

    @PostMapping("/me/location")
    public void updateLocation(@RequestHeader("X-User-Id") Long userId,
                                @RequestBody LocationUpdateDto dto) {
        service.updateLocation(userId, dto);
    }

    @PostMapping("/me/availability")
    public void setAvailability(@RequestHeader("X-User-Id") Long userId, @RequestParam boolean available) {
        service.setAvailability(userId, available);
    }
}
