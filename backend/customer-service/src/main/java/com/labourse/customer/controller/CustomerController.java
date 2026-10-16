package com.labourse.customer.controller;

import com.labourse.customer.dto.CustomerProfileDto;
import com.labourse.customer.entity.CustomerProfile;
import com.labourse.customer.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerProfileService service;

    @GetMapping("/me")
    public CustomerProfile getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        return service.getByUserId(userId);
    }

    @PutMapping("/me")
    public CustomerProfile updateMyProfile(@RequestHeader("X-User-Id") Long userId,
                                            @RequestBody CustomerProfileDto dto) {
        return service.update(userId, dto);
    }
}
