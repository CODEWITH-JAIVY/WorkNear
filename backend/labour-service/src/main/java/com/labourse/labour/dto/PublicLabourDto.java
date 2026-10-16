package com.labourse.labour.dto;

import java.util.List;

// What a customer may see about a labour who has shown interest in their job. Deliberately
// excludes location (current lat/lon), gender, and anything contact-related — the customer
// chooses on name, skills, rating, rate and KYC status, and talks via in-app chat.
public record PublicLabourDto(
        Long userId,
        String name,
        String labourType,
        List<String> serviceCodes,
        double rating,
        int ratingCount,
        boolean kycVerified,
        String city,
        List<String> languages,
        Double rate,
        String about
) {}
