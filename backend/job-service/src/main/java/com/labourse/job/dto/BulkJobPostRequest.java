package com.labourse.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkJobPostRequest {
    @NotBlank private String requiredLabourType;
    @Min(1) private int workersNeeded;
    private String description;
    private String siteAddressText;
    @NotNull private Double latitude;
    @NotNull private Double longitude;
    private Double radiusKm = 5.0;
    private String externalFormLink; // optional — Google Form or any external application link
}
