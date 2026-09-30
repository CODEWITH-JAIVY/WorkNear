package com.labourse.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents", uniqueConstraints = @UniqueConstraint(columnNames = {"labourId", "docType"}))
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long labourId;

    @Enumerated(EnumType.STRING)
    private DocType docType;

    private String documentUrl; // uploaded via media-service, URL passed in here

    @Enumerated(EnumType.STRING)
    private KycStatus status = KycStatus.PENDING;

    private String reviewNotes;
    private Long reviewedByAdminId;

    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
}
