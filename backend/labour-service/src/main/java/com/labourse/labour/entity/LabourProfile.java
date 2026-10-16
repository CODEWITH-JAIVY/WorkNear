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

    // Catalog ServiceItem codes this labour offers, e.g. "HOME_REPAIR__ELECTRICIAN,CLEANING__KITCHEN_CLEANING".
    // THIS is what matching-service filters a job's single requiredServiceCode against — replaces
    // the old single labourType exact-match. Multiple codes = multiple trades/skills on one profile.
    @Column(length = 2000)
    private String serviceCodesCsv;

    @Column(length = 500)
    private String skillsCsv;       // "pipe,tap,leak" — kept simple; move to a join table if skills grow complex

    @Column(length = 500)
    private String subSkillsCsv;    // e.g. "KITCHEN_CLEANING,BATHROOM_CLEANING" — trade-specific sub-category

    @Enumerated(EnumType.STRING)
    private Gender gender;          // customer-side gender preference matches against this, when set

    @Column(length = 200)
    private String languagesCsv;    // e.g. "HINDI,ENGLISH"

    private Double rate;            // labour's quoted rate — used for budget filtering, optional

    private String about;
    private boolean availableToday;
    private double rating = 0.0;
    private int ratingCount = 0;

    private String city;
    private Double currentLat;
    private Double currentLon;

    private boolean kycVerified = false;
}
