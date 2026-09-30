package com.labourse.job.dto;

import lombok.Data;

@Data
public class NearbyLabourDto {
    private String labourId;
    private double distanceKm;
    private double rating;
}
