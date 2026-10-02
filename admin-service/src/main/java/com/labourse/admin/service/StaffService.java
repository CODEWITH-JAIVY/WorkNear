package com.labourse.admin.service;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.common.PageResponse;
import com.labourse.admin.dto.AdminDtos.CreateStaffRequest;
import com.labourse.admin.dto.AdminDtos.CreatedStaffResponse;
import com.labourse.admin.dto.AdminDtos.StaffView;
import com.labourse.admin.entity.Staff;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.repository.RefreshTokenRepository;
import com.labourse.admin.repository.StaffRepository;
import com.labourse.admin.security.PasswordPolicy;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.security.StaffRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @Transactional
    public CreatedStaffResponse create(StaffPrincipal actor, CreateStaffRequest req) {
        String email = req.email().trim().toLowerCase();
        if (staffRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("A staff member with this email already exists");
        }
        String temporaryPassword = PasswordPolicy.generateTemporaryPassword();

        Staff staff = new Staff();
        staff.setName(req.name().trim());
        staff.setEmail(email);
        staff.setRole(req.role());
        staff.setDepartment(req.department());
        staff.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        staff.setMustChangePassword(true);
        staff.setCreatedBy(actor.id());
        staffRepository.save(staff);

        audit.record(actor, "STAFF_CREATED", "STAFF", staff.getId(), "email=" + email + ", role=" + req.role());
        return new CreatedStaffResponse(StaffView.from(staff), temporaryPassword);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffView> list(int page, int size) {
        return PageResponse.of(staffRepository.findAll(PageResponse.pageable(page, size)).map(StaffView::from));
    }

    @Transactional(readOnly = true)
    public StaffView get(Long id) {
        return StaffView.from(find(id));
    }

    @Transactional
    public StaffView changeRole(StaffPrincipal actor, Long id, StaffRole newRole) {
        if (actor.id().equals(id)) {
            throw ApiException.badRequest("You cannot change your own role");
        }
        Staff staff = find(id);
        if (staff.getRole() == StaffRole.SUPER_ADMIN && newRole != StaffRole.SUPER_ADMIN) {
            assertNotLastSuperAdmin(staff);
        }
        StaffRole old = staff.getRole();
        staff.setRole(newRole);
        staffRepository.save(staff);
        audit.record(actor, "STAFF_ROLE_CHANGED", "STAFF", id, old + " -> " + newRole);
        return StaffView.from(staff);
    }

    @Transactional
    public StaffView disable(StaffPrincipal actor, Long id) {
        if (actor.id().equals(id)) {
            throw ApiException.badRequest("You cannot disable your own account");
        }
        Staff staff = find(id);
        if (staff.getStatus() == StaffStatus.DISABLED) {
            throw ApiException.conflict("Staff member is already disabled");
        }
        assertNotLastSuperAdmin(staff);
        staff.setStatus(StaffStatus.DISABLED);
        staff.setTokenVersion(staff.getTokenVersion() + 1);
        staffRepository.save(staff);
        refreshTokenRepository.revokeAllForStaff(id);
        audit.record(actor, "STAFF_DISABLED", "STAFF", id, "email=" + staff.getEmail());
        return StaffView.from(staff);
    }

    @Transactional
    public StaffView enable(StaffPrincipal actor, Long id) {
        Staff staff = find(id);
        if (staff.getStatus() == StaffStatus.ACTIVE) {
            throw ApiException.conflict("Staff member is already active");
        }
        staff.setStatus(StaffStatus.ACTIVE);
        staff.setFailedLoginAttempts(0);
        staff.setLockedUntil(null);
        staffRepository.save(staff);
        audit.record(actor, "STAFF_ENABLED", "STAFF", id, "email=" + staff.getEmail());
        return StaffView.from(staff);
    }

    @Transactional
    public CreatedStaffResponse resetPassword(StaffPrincipal actor, Long id) {
        Staff staff = find(id);
        String temporaryPassword = PasswordPolicy.generateTemporaryPassword();
        staff.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        staff.setMustChangePassword(true);
        staff.setTokenVersion(staff.getTokenVersion() + 1);
        staff.setFailedLoginAttempts(0);
        staff.setLockedUntil(null);
        staffRepository.save(staff);
        refreshTokenRepository.revokeAllForStaff(id);
        audit.record(actor, "STAFF_PASSWORD_RESET", "STAFF", id, "email=" + staff.getEmail());
        return new CreatedStaffResponse(StaffView.from(staff), temporaryPassword);
    }

    private Staff find(Long id) {
        return staffRepository.findById(id).orElseThrow(() -> ApiException.notFound("Staff not found"));
    }

    /** Never allow the system to end up with no active SUPER_ADMIN. */
    private void assertNotLastSuperAdmin(Staff staff) {
        if (staff.getRole() == StaffRole.SUPER_ADMIN
                && staff.getStatus() == StaffStatus.ACTIVE
                && staffRepository.countByRoleAndStatus(StaffRole.SUPER_ADMIN, StaffStatus.ACTIVE) <= 1) {
            throw ApiException.conflict("This is the last active SUPER_ADMIN");
        }
    }
}
