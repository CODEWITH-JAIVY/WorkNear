package com.labourse.labour.dto;

import com.labourse.labour.entity.Gender;
import lombok.Data;

@Data
public class LabourProfileDto {
    private String name;
    private String labourType;      // coarse category, display-only now
    private String employmentType;
    private java.util.List<String> serviceCodes; // multiple catalog ServiceItem codes — validated against catalog-service
    private String skillsCsv;
    private String subSkillsCsv;
    private Gender gender;
    private String languagesCsv;
    private Double rate;
    private String about;
    private String city;
}
