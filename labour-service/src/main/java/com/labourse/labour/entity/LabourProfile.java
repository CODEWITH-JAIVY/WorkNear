package com.labourse.labour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "labour_profiles")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class LabourProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String name;
    private String labourType;      // PLUMBER, ELECTRICIAN, CARPENTER, ...
    private String employmentType;  // FULL_TIME, PART_TIME, ONE_TIME

    @Column(length = 500)
    private String skillsCsv;       // "pipe,tap,leak" — kept simple; move to a join table if skills grow complex

    private String about;
    private boolean availableToday;
    private double rating = 0.0;
    private int ratingCount = 0;

    private String city;
    private Double currentLat;
    private Double currentLon;

    private boolean kycVerified = false;
}
