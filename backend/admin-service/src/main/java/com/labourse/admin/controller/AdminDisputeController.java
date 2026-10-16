package com.labourse.admin.controller;

import com.labourse.admin.common.PageResponse;
import com.labourse.admin.dto.AdminDtos.AssignRequest;
import com.labourse.admin.dto.AdminDtos.CommentRequest;
import com.labourse.admin.dto.AdminDtos.DisputeDetail;
import com.labourse.admin.dto.AdminDtos.ResolveRequest;
import com.labourse.admin.entity.Dispute;
import com.labourse.admin.entity.DisputeComment;
import com.labourse.admin.entity.DisputeStatus;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Staff-side dispute handling. Customer/labour executives only see disputes from their own user type. */
@RestController
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISPUTE_VIEW')")
    public PageResponse<Dispute> list(@AuthenticationPrincipal StaffPrincipal me,
                                      @RequestParam(required = false) DisputeStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return disputeService.search(me, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISPUTE_VIEW')")
    public DisputeDetail get(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id) {
        return disputeService.get(me, id);
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('DISPUTE_RESOLVE')")
    public Dispute claim(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id) {
        return disputeService.claim(me, id);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('DISPUTE_ASSIGN')")
    public Dispute assign(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id,
                          @Valid @RequestBody AssignRequest req) {
        return disputeService.assign(me, id, req.staffId());
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('DISPUTE_RESOLVE')")
    public DisputeComment comment(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id,
                                  @Valid @RequestBody CommentRequest req) {
        return disputeService.addComment(me, id, req.text());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('DISPUTE_RESOLVE')")
    public Dispute resolve(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long id,
                           @Valid @RequestBody ResolveRequest req) {
        return disputeService.resolve(me, id, req.notes(), req.accepted());
    }
}
