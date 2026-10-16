package com.labourse.auth.service;

import com.labourse.auth.dto.*;
import com.labourse.auth.entity.*;
import com.labourse.auth.repository.UserRepository;
import com.labourse.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRegisteredEventPublisher eventPublisher;

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalStateException("Email already registered");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setMobile(req.getMobile());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setUserType(req.getUserType());
        user = userRepository.save(user);

        // customer-service / labour-service listens on this topic and creates the profile shell
        eventPublisher.publish(String.valueOf(user.getId()), user.getUserType().name(), user.getEmail());

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!user.isEnabled()) {
            throw new IllegalStateException("This account has been suspended");
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            throw new IllegalArgumentException("Not a refresh token");
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueTokens(user);
    }

    // Called by the frontend's /select-role page after a NEW Google sign-in (no userType yet).
    // This is the point where the profile-shell event actually fires for OAuth users —
    // local signup fires it immediately since userType is known at signup time.
    @Transactional
    public AuthResponse selectRole(Long userId, com.labourse.auth.entity.UserType userType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getUserType() != null) {
            throw new IllegalStateException("Role already set for this user");
        }
        user.setUserType(userType);
        user = userRepository.save(user);
        eventPublisher.publish(String.valueOf(user.getId()), user.getUserType().name(), user.getEmail());
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String role = user.getUserType() == null ? "PENDING" : user.getUserType().name();
        String access = jwtService.generateAccessToken(user.getId(), role);
        String refresh = jwtService.generateRefreshToken(user.getId());
        return new AuthResponse(access, refresh, user.getId(), role);
    }
}
