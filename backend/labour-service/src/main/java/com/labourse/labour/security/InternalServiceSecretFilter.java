package com.labourse.labour.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Guards /internal/labour/** (rating updates, kyc-verified flag). Same pattern in every
// service that exposes /internal/** — see auth-service's copy for the fuller comment.
@Component
public class InternalServiceSecretFilter extends OncePerRequestFilter {

    @Value("${internal.service.secret}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/")) {
            String provided = request.getHeader("X-Internal-Secret");
            if (provided == null || !provided.equals(expectedSecret)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing or invalid internal service secret");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
