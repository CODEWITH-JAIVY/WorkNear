package com.labourse.admin.controller;

import com.labourse.admin.client.AuthServiceClient;
import com.labourse.admin.entity.Dispute;
import com.labourse.admin.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Every route here should additionally be gated by an ADMIN role check at the gateway
// (extend JwtAuthGatewayFilter to reject non-ADMIN X-User-Role on /api/admin/**)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DisputeService disputeService;
    private final AuthServiceClient authServiceClient;

    @PostMapping("/disputes")
    public Dispute raiseDispute(@RequestHeader("X-User-Id") Long userId,
                                 @RequestParam Long jobId, @RequestParam String reason) {
        return disputeService.raise(jobId, userId, reason);
    }

    @GetMapping("/disputes/open")
    public List<Dispute> openDisputes() {
        return disputeService.openDisputes();
    }

    @PostMapping("/disputes/{id}/resolve")
    public Dispute resolveDispute(@PathVariable Long id, @RequestParam String notes, @RequestParam boolean accepted) {
        return disputeService.resolve(id, notes, accepted);
    }

    @PostMapping("/users/{userId}/ban")
    public void banUser(@PathVariable Long userId) {
        authServiceClient.banUser(userId);
    }
}
