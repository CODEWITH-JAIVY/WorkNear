package com.labourse.admin.controller;

import com.labourse.admin.dto.AdminDtos.BanRequest;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.service.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasAuthority('USER_BAN')")
    public ResponseEntity<Void> ban(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long userId,
                                    @Valid @RequestBody BanRequest req) {
        moderationService.ban(me, userId, req.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasAuthority('USER_UNBAN')")
    public ResponseEntity<Void> unban(@AuthenticationPrincipal StaffPrincipal me, @PathVariable Long userId,
                                      @Valid @RequestBody BanRequest req) {
        moderationService.unban(me, userId, req.reason());
        return ResponseEntity.noContent().build();
    }
}
