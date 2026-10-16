package com.labourse.job.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkJobFeedResponse {
    private List<BulkJobFeedItemDto> jobs;
    private Object adSlot; // always null today — placeholder for banner/native/interstitial ad data
}
