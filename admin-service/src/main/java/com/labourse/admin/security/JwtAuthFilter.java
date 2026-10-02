package com.labourse.admin.security;

import com.labourse.admin.entity.Staff;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.repository.StaffRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Not a @Component on purpose: it is created inside SecurityConfig so Spring Boot does not
 * register it a second time as a plain servlet filter.
 *
 * The staff row is loaded on every request, so disabling a staff member, changing their role or
 * bumping tokenVersion takes effect immediately instead of after the 15 minute token expiry.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StaffRepository staffRepository;

    public JwtAuthFilter(JwtService jwtService, StaffRepository staffRepository) {
        this.jwtService = jwtService;
        this.staffRepository = staffRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7).trim());
                if ("STAFF".equals(claims.get("kind", String.class))) {
                    Long staffId = Long.valueOf(claims.getSubject());
                    Integer tokenVersion = claims.get("tv", Integer.class);
                    Staff staff = staffRepository.findById(staffId).orElse(null);
                    if (staff != null
                            && staff.getStatus() == StaffStatus.ACTIVE
                            && tokenVersion != null
                            && tokenVersion.intValue() == staff.getTokenVersion()) {
                        authenticate(staff);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                // invalid / expired token: leave the request unauthenticated, entry point returns 401
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(Staff staff) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (staff.isMustChangePassword()) {
            // until the temporary password is changed the account can only reach /auth/me, /auth/change-password
            authorities.add(new SimpleGrantedAuthority("PASSWORD_CHANGE_ONLY"));
        } else {
            staff.getRole().permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.name())));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + staff.getRole().name()));
        }
        StaffPrincipal principal = new StaffPrincipal(staff.getId(), staff.getEmail(), staff.getName(), staff.getRole());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
