package com.labourse.job.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// One row per labour who has expressed interest in a job — the browse-and-choose model's core
// table. Replaces the old "first labour to accept wins" flow: now many labour can be interested
// at once, the customer views/chats/calls, and picks one (see JobService.selectLabour).
@Entity
@Table(name = "job_interests", uniqueConstraints = @UniqueConstraint(columnNames = {"jobId", "labourId"}))
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class JobInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Long labourId;

    @Column(length = 300)
    private String message; // optional — labour can leave a short note ("Can come by 4pm, ₹350")

    private LocalDateTime createdAt = LocalDateTime.now();
}
