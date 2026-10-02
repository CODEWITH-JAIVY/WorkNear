package com.labourse.admin.controller;

import com.labourse.admin.common.PageResponse;
import com.labourse.admin.entity.AuditLog;
import com.labourse.admin.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public PageResponse<AuditLog> search(@RequestParam(required = false) Long actorId,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return auditService.search(actorId, action, page, size);
    }
}
