package com.labourse.admin.dto;

import com.labourse.admin.entity.Dispute;
import com.labourse.admin.entity.DisputeComment;
import com.labourse.admin.entity.DisputeStatus;
import com.labourse.admin.entity.Staff;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.security.Permission;
import com.labourse.admin.security.StaffRole;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminDtos {

    private AdminDtos() {}

    // ---- auth ----
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds,
                                boolean mustChangePassword, StaffView staff) {}

    // ---- staff ----
    public record StaffView(Long id, String name, String email, StaffRole role, String department,
                            StaffStatus status, boolean mustChangePassword, LocalDateTime lastLoginAt,
                            LocalDateTime createdAt, Set<Permission> permissions) {
        public static StaffView from(Staff s) {
            return new StaffView(s.getId(), s.getName(), s.getEmail(), s.getRole(), s.getDepartment(),
                    s.getStatus(), s.isMustChangePassword(), s.getLastLoginAt(), s.getCreatedAt(),
                    s.getRole().permissions());
        }
    }

    public record CreateStaffRequest(@NotBlank @Size(max = 100) String name,
                                     @NotBlank @Email @Size(max = 150) String email,
                                     @NotNull StaffRole role,
                                     @Size(max = 100) String department) {}

    /** The temporary password is returned exactly once and is never stored in plain text. */
    public record CreatedStaffResponse(StaffView staff, String temporaryPassword) {}

    public record ChangeRoleRequest(@NotNull StaffRole role) {}

    // ---- disputes ----
    public record RaiseDisputeRequest(@NotNull Long jobId, @NotBlank @Size(max = 255) String reason) {}

    /** What an end user sees: no internal staff ids. */
    public record UserDisputeView(Long id, Long jobId, String reason, DisputeStatus status,
                                  String resolutionNotes, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        public static UserDisputeView from(Dispute d) {
            return new UserDisputeView(d.getId(), d.getJobId(), d.getReason(), d.getStatus(),
                    d.getResolutionNotes(), d.getCreatedAt(), d.getResolvedAt());
        }
    }

    public record AssignRequest(@NotNull Long staffId) {}

    public record CommentRequest(@NotBlank @Size(max = 1000) String text) {}

    public record ResolveRequest(@NotBlank @Size(max = 255) String notes, @NotNull Boolean accepted) {}

    public record DisputeDetail(Dispute dispute, List<DisputeComment> comments) {}

    // ---- moderation ----
    public record BanRequest(@NotBlank @Size(max = 255) String reason) {}

    // ---- refunds ----
    public record RefundCreateRequest(@NotNull Long jobId, Long disputeId,
                                      @NotNull @DecimalMin("0.01") BigDecimal amount,
                                      @NotBlank @Size(max = 255) String reason) {}

    public record RefundDecisionRequest(@NotBlank @Size(max = 255) String notes) {}

    // ---- dashboard ----
    public record DashboardResponse(Map<String, Long> disputesByStatus, long pendingRefunds, long activeStaff) {}
}
