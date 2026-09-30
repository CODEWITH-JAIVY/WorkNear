package com.labourse.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewRequest {
    @NotNull private Long jobId;
    @NotNull private Long revieweeId;
    @NotNull private String reviewerType; // CUSTOMER | LABOUR
    @Min(1) @Max(5) private int rating;
    private String comment;
}
