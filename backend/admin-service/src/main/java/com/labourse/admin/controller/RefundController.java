package com.labourse.admin.controller;

import com.labourse.admin.common.PageResponse;
import com.labourse.admin.dto.AdminDtos.RefundCreateRequest;
import com.labourse.admin.dto.AdminDtos.RefundDecisionRequest;
import com.labourse.admin.entity.RefundRequest;
import com.labourse.admin.entity.RefundStatus;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @PreAuthorize("hasAuthority('REFUND_REQUEST')")
    public ResponseEntity<RefundRequest> create(@AuthenticationPrincipal StaffPrincipal me,
                                                @Valid @RequestBody RefundCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.create(me, req));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REFUND_REQUEST','REFUND_APPROVE')")
    public PageResponse<RefundRequest> list(@RequestParam(required = false) RefundStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return refundService.list(status, page, size);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public RefundRequest approve(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id,
                                 @Valid @RequestBody RefundDecisionRequest req) {
        return refundService.approve(me, id, req.notes());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public RefundRequest reject(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id,
                                @Valid @RequestBody RefundDecisionRequest req) {
        return refundService.reject(me, id, req.notes());
    }
}
