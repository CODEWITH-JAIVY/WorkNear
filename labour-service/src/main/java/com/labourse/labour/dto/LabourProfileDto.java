package com.labourse.labour.dto;

import lombok.Data;

@Data
public class LabourProfileDto {
    private String name;
    private String labourType;
    private String employmentType;
    private String skillsCsv;
    private String about;
    private String city;
}
