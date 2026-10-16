package com.labourse.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Only the SHA-256 hash of the refresh token is stored. */
@Entity
@Table(name = "staff_refresh_tokens")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long staffId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt = LocalDateTime.now();
}
