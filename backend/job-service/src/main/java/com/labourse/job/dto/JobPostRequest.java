package com.labourse.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class JobPostRequest {
    @NotBlank private String title;
    private String description;
    @NotBlank private String requiredLabourType;
    private List<String> skillsRequired;
    private String subSkill;            // e.g. "KITCHEN_CLEANING" — optional, trade-specific
    private String genderPreference;    // "MALE" | "FEMALE" | null — optional customer preference
    private String languagePreference;  // optional
    private java.time.LocalDateTime scheduledAt;   // optional — null means "as soon as possible"
    private List<String> mediaKeys;                // keys returned by media-service /upload, max 5
    private Double minRating;
    @NotNull private Double latitude;
    @NotNull private Double longitude;
    private String addressText;
    private Double budget;              // doubles as the matching max-budget filter — see JobService
    private Double radiusKm = 5.0;
}
