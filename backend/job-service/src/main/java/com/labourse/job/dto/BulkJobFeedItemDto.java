package com.labourse.job.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BulkJobFeedItemDto {
    private Long id;
    private String requiredLabourType;
    private int workersNeeded;
    private String description;
    private String siteAddressText;
    private String externalFormLink; // null → worker applies in-app instead (see /interest)
    private LocalDateTime createdAt;
}
