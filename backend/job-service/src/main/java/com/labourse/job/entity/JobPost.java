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
    private String subSkill;
    private String genderPreference;
    private String languagePreference;
    private Double minRating;

    // "Date & Time" step of the post flow — when the customer wants the work done
    private LocalDateTime scheduledAt;

    // S3 object keys of the customer's photos/video/voice-note. Kept as keys (not URLs) so
    // media-service can delete them when the job completes — see JobService.completeJob.
    @Column(length = 2000)
    private String mediaKeysCsv;

    private double latitude;
    private double longitude;
    private String addressText;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    private Double budget;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime acceptedAt;
}
