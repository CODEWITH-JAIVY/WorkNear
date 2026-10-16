package com.labourse.admin.service;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.dto.AdminDtos.StaffView;
import com.labourse.admin.dto.AdminDtos.TokenResponse;
import com.labourse.admin.entity.RefreshToken;
import com.labourse.admin.entity.Staff;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.repository.RefreshTokenRepository;
import com.labourse.admin.repository.StaffRepository;
import com.labourse.admin.security.JwtService;
import com.labourse.admin.security.PasswordPolicy;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StaffRepository staffRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;

    @Value("${admin.jwt.refresh-ttl-days:7}")
    private long refreshTtlDays;

    @Value("${admin.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${admin.security.lock-minutes:15}")
    private long lockMinutes;

    /**
     * Deliberately NOT @Transactional: failed-login counters and audit rows must persist
     * even though the method ends by throwing.
     */
    public TokenResponse login(String email, String password) {
        String normalized = email.trim().toLowerCase();
        Staff staff = staffRepository.findByEmailIgnoreCase(normalized).orElse(null);

        if (staff == null) {
            audit.recordAnonymous(normalized, "LOGIN_FAILED", "unknown email");
            throw ApiException.unauthorized("Invalid credentials");
        }

        LocalDateTime now = LocalDateTime.now();
        if (staff.getLockedUntil() != null && staff.getLockedUntil().isAfter(now)) {
            audit.recordAnonymous(normalized, "LOGIN_BLOCKED", "account locked");
            throw new ApiException(HttpStatus.LOCKED, "Account temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(password, staff.getPasswordHash())) {
            int attempts = staff.getFailedLoginAttempts() + 1;
            if (attempts >= maxFailedAttempts) {
                staff.setLockedUntil(now.plusMinutes(lockMinutes));
                staff.setFailedLoginAttempts(0);
            } else {
                staff.setFailedLoginAttempts(attempts);
            }
            staffRepository.save(staff);
            audit.recordAnonymous(normalized, "LOGIN_FAILED", "wrong password, attempt " + attempts);
            throw ApiException.unauthorized("Invalid credentials");
        }

        if (staff.getStatus() != StaffStatus.ACTIVE) {
            audit.recordAnonymous(normalized, "LOGIN_BLOCKED", "account disabled");
            throw ApiException.forbidden("Account disabled");
        }

        staff.setFailedLoginAttempts(0);
        staff.setLockedUntil(null);
        staff.setLastLoginAt(now);
        staffRepository.save(staff);
        audit.record(toPrincipal(staff), "LOGIN_SUCCESS", "STAFF", staff.getId(), null);
        return issueTokens(staff);
    }

    /** noRollbackFor: on refresh-token reuse we revoke everything and must keep that change. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllForStaff(stored.getStaffId());
            throw ApiException.unauthorized("Refresh token reuse detected. Please log in again.");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.unauthorized("Refresh token expired. Please log in again.");
        }

        Staff staff = staffRepository.findById(stored.getStaffId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw ApiException.forbidden("Account disabled");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(staff);
    }

    /** Idempotent: unknown or already revoked tokens are ignored. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken)).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public TokenResponse changePassword(StaffPrincipal me, String oldPassword, String newPassword) {
        Staff staff = staffRepository.findById(me.id()).orElseThrow(() -> ApiException.notFound("Staff not found"));
        if (!passwordEncoder.matches(oldPassword, staff.getPasswordHash())) {
            throw ApiException.badRequest("Old password is incorrect");
        }
        PasswordPolicy.validate(newPassword);
        if (oldPassword.equals(newPassword)) {
            throw ApiException.badRequest("New password must be different from the old one");
        }

        staff.setPasswordHash(passwordEncoder.encode(newPassword));
        staff.setMustChangePassword(false);
        staff.setTokenVersion(staff.getTokenVersion() + 1);   // kills all older access tokens
        staffRepository.save(staff);
        refreshTokenRepository.revokeAllForStaff(staff.getId());
        audit.record(me, "PASSWORD_CHANGED", "STAFF", staff.getId(), null);
        return issueTokens(staff);
    }

    @Transactional(readOnly = true)
    public StaffView me(StaffPrincipal me) {
        return staffRepository.findById(me.id()).map(StaffView::from)
                .orElseThrow(() -> ApiException.notFound("Staff not found"));
    }

    private TokenResponse issueTokens(Staff staff) {
        String rawRefresh = TokenHasher.newOpaqueToken();
        RefreshToken token = new RefreshToken();
        token.setStaffId(staff.getId());
        token.setTokenHash(TokenHasher.sha256Hex(rawRefresh));
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshTtlDays));
        refreshTokenRepository.save(token);

        return new TokenResponse(jwtService.createAccessToken(staff), rawRefresh,
                jwtService.accessTtlSeconds(), staff.isMustChangePassword(), StaffView.from(staff));
    }

    private StaffPrincipal toPrincipal(Staff s) {
        return new StaffPrincipal(s.getId(), s.getEmail(), s.getName(), s.getRole());
    }
}
