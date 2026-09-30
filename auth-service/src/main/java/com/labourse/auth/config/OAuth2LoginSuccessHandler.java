package com.labourse.auth.config;

import com.labourse.auth.entity.AuthProvider;
import com.labourse.auth.entity.User;
import com.labourse.auth.repository.UserRepository;
import com.labourse.auth.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        boolean isNewUser = userRepository.findByEmail(email).isEmpty();

        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setAuthProvider(AuthProvider.GOOGLE);
        if (user.getId() == null) {
            user.setEnabled(true);
        }
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getId(),
                user.getUserType() == null ? "PENDING" : user.getUserType().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // New Google sign-ins have no userType yet — frontend's SelectRole page collects it,
        // then calls POST /api/auth/select-role, which is what actually publishes user.registered.
        String redirectUrl = frontendUrl + (isNewUser || user.getUserType() == null
                ? "/select-role?token=" + accessToken
                : "/oauth-success?token=" + accessToken + "&refreshToken=" + refreshToken);

        response.sendRedirect(redirectUrl);
    }
}
