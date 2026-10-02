package com.labourse.customer.service;

import com.labourse.customer.dto.CustomerProfileDto;
import com.labourse.customer.entity.CustomerProfile;
import com.labourse.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository repository;

    public CustomerProfile getByUserId(Long userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found for user " + userId));
    }

    public CustomerProfile update(Long userId, CustomerProfileDto dto) {
        CustomerProfile profile = getByUserId(userId);
        profile.setName(dto.getName());
        profile.setProfileImageUrl(dto.getProfileImageUrl());
        profile.setAddressLine(dto.getAddressLine());
        profile.setCity(dto.getCity());
        profile.setPincode(dto.getPincode());
        return repository.save(profile);
    }
}
