package com.labourse.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;
    private String actorEmail;
    private String actorRole;

    @Column(length = 60)
    private String action;

    @Column(length = 60)
    private String targetType;

    @Column(length = 64)
    private String targetId;

    @Column(length = 1000)
    private String details;

    @Column(length = 64)
    private String ipAddress;

    private LocalDateTime createdAt = LocalDateTime.now();
}
