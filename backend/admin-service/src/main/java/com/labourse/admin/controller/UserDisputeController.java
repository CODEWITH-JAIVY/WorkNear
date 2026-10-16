package com.labourse.admin.controller;

import com.labourse.admin.dto.AdminDtos.RaiseDisputeRequest;
import com.labourse.admin.dto.AdminDtos.UserDisputeView;
import com.labourse.admin.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * End-user (CUSTOMER / LABOUR) endpoints. The API gateway must authenticate the user and add
 * X-User-Id / X-User-Role; this service only trusts those headers, so it must not be reachable
 * from outside except through the gateway.
 */
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class UserDisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<UserDisputeView> raise(@RequestHeader("X-User-Id") Long userId,
                                                 @RequestHeader(value = "X-User-Role", required = false, defaultValue = "UNKNOWN") String role,
                                                 @Valid @RequestBody RaiseDisputeRequest req) {
        var dispute = disputeService.raise(req.jobId(), userId, role.trim().toUpperCase(), req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDisputeView.from(dispute));
    }

    @GetMapping("/mine")
    public List<UserDisputeView> mine(@RequestHeader("X-User-Id") Long userId) {
        return disputeService.mine(userId).stream().map(UserDisputeView::from).toList();
    }
}
