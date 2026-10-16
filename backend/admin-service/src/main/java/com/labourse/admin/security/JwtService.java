package com.labourse.admin.security;

import com.labourse.admin.entity.Staff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and parses STAFF access tokens. Signed with admin.jwt.secret (separate from auth-service's secret). */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlMinutes;

    public JwtService(@Value("${admin.jwt.secret}") String secret,
                      @Value("${admin.jwt.access-ttl-minutes:15}") long accessTtlMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("admin.jwt.secret (ADMIN_JWT_SECRET) must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public String createAccessToken(Staff staff) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(staff.getId()))
                .claim("kind", "STAFF")
                .claim("tv", staff.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(accessTtlMinutes))))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long accessTtlSeconds() {
        return accessTtlMinutes * 60;
    }
}
