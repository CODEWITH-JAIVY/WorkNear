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
    private Double minRating;
    @NotNull private Double latitude;
    @NotNull private Double longitude;
    private String addressText;
    private Double budget;
    private Double radiusKm = 5.0;
}
