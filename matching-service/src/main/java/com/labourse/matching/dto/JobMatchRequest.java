package com.labourse.matching.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobMatchRequest {
    private String jobId;
    private double latitude;
    private double longitude;
    private String requiredLabourType;
    private List<String> skillsRequired;
    private Double minRating;
    private Double radiusKm = 5.0;
    private Integer maxCandidates = 20;
}
