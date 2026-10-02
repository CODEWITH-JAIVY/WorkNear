package com.labourse.admin.security;

import com.labourse.admin.entity.Staff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-chars-long";

    private Staff staff() {
        Staff s = new Staff();
        s.setId(42L);
        s.setTokenVersion(3);
        return s;
    }

    @Test
    void tokenRoundTripKeepsSubjectKindAndVersion() {
        JwtService jwt = new JwtService(SECRET, 15);

        Claims claims = jwt.parse(jwt.createAccessToken(staff()));

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("kind", String.class)).isEqualTo("STAFF");
        assertThat(claims.get("tv", Integer.class)).isEqualTo(3);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secret-that-is-also-32-chars-plus!!", 15).createAccessToken(staff());

        assertThatThrownBy(() -> new JwtService(SECRET, 15).parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService("too-short", 15)).isInstanceOf(IllegalStateException.class);
    }
}
