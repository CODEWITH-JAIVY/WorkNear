package com.labourse.rating.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Reviews are bidirectional — customer rates labour, labour rates customer — reviewerType tells which
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"jobId", "reviewerId"}))
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Long reviewerId;
    private Long revieweeId;
    private String reviewerType; // CUSTOMER or LABOUR

    private int rating; // 1-5
    @Column(length = 500)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}
