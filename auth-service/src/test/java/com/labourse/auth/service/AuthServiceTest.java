package com.labourse.auth.service;

import com.labourse.auth.dto.LoginRequest;
import com.labourse.auth.dto.SignupRequest;
import com.labourse.auth.entity.User;
import com.labourse.auth.entity.UserType;
import com.labourse.auth.repository.UserRepository;
import com.labourse.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock UserRegisteredEventPublisher eventPublisher;

    @InjectMocks AuthService authService;

    @Test
    void signup_savesUserAndPublishesEvent_whenEmailNotTaken() {
        SignupRequest req = new SignupRequest();
        req.setEmail("labour@example.com");
        req.setMobile("9999999999");
        req.setPassword("Passw0rd!");
        req.setUserType(UserType.LABOUR);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateAccessToken(1L, "LABOUR")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(1L)).thenReturn("refresh-token");

        var response = authService.signup(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUserType()).isEqualTo("LABOUR");
        verify(eventPublisher).publish("1", "LABOUR", req.getEmail());
    }

    @Test
    void signup_throws_whenEmailAlreadyRegistered() {
        SignupRequest req = new SignupRequest();
        req.setEmail("taken@example.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void login_throws_whenPasswordDoesNotMatch() {
        LoginRequest req = new LoginRequest();
        req.setEmail("customer@example.com");
        req.setPassword("wrongPassword");

        User existing = new User();
        existing.setId(2L);
        existing.setEmail(req.getEmail());
        existing.setPasswordHash("hashed");
        existing.setEnabled(true);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(req.getPassword(), "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_throws_whenAccountIsBanned() {
        LoginRequest req = new LoginRequest();
        req.setEmail("banned@example.com");
        req.setPassword("Passw0rd!");

        User existing = new User();
        existing.setId(3L);
        existing.setEmail(req.getEmail());
        existing.setPasswordHash("hashed");
        existing.setEnabled(false); // banned by admin-service

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(req.getPassword(), "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("suspended");
    }
}
