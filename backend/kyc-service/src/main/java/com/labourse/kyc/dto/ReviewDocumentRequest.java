package com.labourse.kyc.dto;

import lombok.Data;

@Data
public class ReviewDocumentRequest {
    private boolean approved;
    private String notes;
}
