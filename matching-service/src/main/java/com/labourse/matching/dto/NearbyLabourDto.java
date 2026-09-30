package com.labourse.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NearbyLabourDto {
    private String labourId;
    private double distanceKm;
    private double rating;
}
