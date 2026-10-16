package com.labourse.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_comments")
@Getter @Setter @NoArgsConstructor
public class DisputeComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long disputeId;
    private Long staffId;
    private String staffName;

    @Column(length = 1000)
    private String body;

    private LocalDateTime createdAt = LocalDateTime.now();
}
