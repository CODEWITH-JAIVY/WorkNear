package com.labourse.job.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_posts")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;      // FK to auth-service User.id
    private Long acceptedLabourId; // set once someone accepts

    private String title;
    private String description;
    private String requiredLabourType;
    @Column(length = 300)
    private String skillsRequiredCsv;
    private Double minRating;

    private double latitude;
    private double longitude;
    private String addressText;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    private Double budget;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime acceptedAt;
}
