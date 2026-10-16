package com.labourse.admin.controller;

import com.labourse.admin.dto.AdminDtos.ChangePasswordRequest;
import com.labourse.admin.dto.AdminDtos.LoginRequest;
import com.labourse.admin.dto.AdminDtos.RefreshRequest;
import com.labourse.admin.dto.AdminDtos.StaffView;
import com.labourse.admin.dto.AdminDtos.TokenResponse;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Works even while mustChangePassword is true. Returns fresh tokens. */
    @PostMapping("/change-password")
    public TokenResponse changePassword(@AuthenticationPrincipal StaffPrincipal me,
                                        @Valid @RequestBody ChangePasswordRequest req) {
        return authService.changePassword(me, req.oldPassword(), req.newPassword());
    }

    @GetMapping("/me")
    public StaffView me(@AuthenticationPrincipal StaffPrincipal me) {
        return authService.me(me);
    }
}
