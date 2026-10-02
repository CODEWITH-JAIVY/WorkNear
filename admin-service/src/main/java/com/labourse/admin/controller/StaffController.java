package com.labourse.admin.controller;

import com.labourse.admin.common.PageResponse;
import com.labourse.admin.dto.AdminDtos.ChangeRoleRequest;
import com.labourse.admin.dto.AdminDtos.CreateStaffRequest;
import com.labourse.admin.dto.AdminDtos.CreatedStaffResponse;
import com.labourse.admin.dto.AdminDtos.StaffView;
import com.labourse.admin.security.Permission;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.security.StaffRole;
import com.labourse.admin.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<CreatedStaffResponse> create(@AuthenticationPrincipal StaffPrincipal me,
                                                       @Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(me, req));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public PageResponse<StaffView> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return staffService.list(page, size);
    }

    /** Every role with its permissions, useful for building an admin UI. */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public Map<StaffRole, Set<Permission>> roles() {
        Map<StaffRole, Set<Permission>> result = new LinkedHashMap<>();
        for (StaffRole role : StaffRole.values()) {
            result.put(role, role.permissions());
        }
        return result;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public StaffView get(@PathVariable Long id) {
        return staffService.get(id);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public StaffView changeRole(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id,
                                @Valid @RequestBody ChangeRoleRequest req) {
        return staffService.changeRole(me, id, req.role());
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public StaffView disable(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id) {
        return staffService.disable(me, id);
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public StaffView enable(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id) {
        return staffService.enable(me, id);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public CreatedStaffResponse resetPassword(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id) {
        return staffService.resetPassword(me, id);
    }
}
