package com.labourse.auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalServiceSecretFilterTest {

    @Mock FilterChain filterChain;

    private InternalServiceSecretFilter filter() {
        InternalServiceSecretFilter f = new InternalServiceSecretFilter();
        ReflectionTestUtils.setField(f, "expectedSecret", "correct-secret");
        return f;
    }

    @Test
    void rejects_whenSecretHeaderMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("PUT", "/internal/auth/5/ban");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter().doFilterInternal(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(403);
        verifyNoInteractions(filterChain);
    }

    @Test
    void rejects_whenSecretHeaderWrong() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("PUT", "/internal/auth/5/ban");
        req.addHeader("X-Internal-Secret", "guessed-wrong-secret");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter().doFilterInternal(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(403);
        verifyNoInteractions(filterChain);
    }

    @Test
    void allows_whenSecretHeaderCorrect() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("PUT", "/internal/auth/5/ban");
        req.addHeader("X-Internal-Secret", "correct-secret");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter().doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }

    @Test
    void ignoresNonInternalPaths_regardlessOfSecret() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter().doFilterInternal(req, res, filterChain);

        // Public API routes must never be blocked by this filter
        verify(filterChain).doFilter(req, res);
    }
}
