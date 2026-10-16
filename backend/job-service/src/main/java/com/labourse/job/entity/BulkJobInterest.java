package com.labourse.job.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Fallback path when a BulkJobPost has no externalFormLink — a lightweight "I'm interested"
// mark so the contractor can still see who to contact, without building a full chat/select flow
// for what is often a one-time, ad-hoc hiring need.
@Entity
@Table(name = "bulk_job_interests", uniqueConstraints = @UniqueConstraint(columnNames = {"bulkJobId", "labourId"}))
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class BulkJobInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bulkJobId;
    private Long labourId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
