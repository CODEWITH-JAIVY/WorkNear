package com.labourse.auth.controller;

import com.labourse.auth.entity.User;
import com.labourse.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// Internal, service-to-service only — same pattern as labour-service's /internal/labour/**.
// Not routed through the public gateway; needs mTLS/shared-secret guarding before real prod use.
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;

    @PutMapping("/{userId}/ban")
    public void ban(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(false);
        userRepository.save(user);
    }
}
