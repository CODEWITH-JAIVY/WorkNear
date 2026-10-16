package com.labourse.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Guards every /internal/** route. The gateway never routes to /internal/** (see api-gateway's
// route list — there's no internal-service route), so in practice this is defense-in-depth
// against anything that reaches this container directly on the docker/k8s network. This is a
// stopgap — see README's "mTLS" section for the real production answer.
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
