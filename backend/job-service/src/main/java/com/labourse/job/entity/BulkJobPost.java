package com.labourse.job.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_job_posts")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class BulkJobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contractorId; // same CUSTOMER user-type — no separate contractor signup

    private String requiredLabourType;
    private int workersNeeded;

    @Column(length = 1000)
    private String description;

    private String siteAddressText;
    private double latitude;
    private double longitude;
    private Double radiusKm = 5.0;

    // Contractor's own Google Form (or any external link) — workers apply there, not in-app.
    // Null is valid: falls back to a lightweight in-app "interested" mark instead (see service).
    private String externalFormLink;

    @Enumerated(EnumType.STRING)
    private BulkJobStatus status = BulkJobStatus.OPEN;

    private LocalDateTime createdAt = LocalDateTime.now();
}
