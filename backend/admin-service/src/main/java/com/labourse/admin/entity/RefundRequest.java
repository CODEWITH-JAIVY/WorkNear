package com.labourse.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Maker-checker: one staff member requests, a different one approves. */
@Entity
@Table(name = "refund_requests")
@Getter @Setter @NoArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Long disputeId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private String reason;

    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.PENDING;

    private Long requestedByStaffId;
    private Long decidedByStaffId;
    private String decisionNotes;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime decidedAt;
}
