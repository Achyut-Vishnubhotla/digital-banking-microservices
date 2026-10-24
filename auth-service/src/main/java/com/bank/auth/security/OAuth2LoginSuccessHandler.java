package com.bank.auth.security;

import com.bank.auth.dto.AuthDtos.AuthResponse;
import com.bank.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Runs after GitHub has authenticated the user (OAuth2 authorization-code flow).
 * We map the GitHub identity to a local user, issue OUR OWN access/refresh JWTs, and send the browser
 * back to the frontend. Tokens go in the URL fragment (#...), which is never sent to a server or logged.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(AuthService authService, @Value("${app.frontend-url}") String frontendUrl) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Number githubId = principal.getAttribute("id");
        String githubLogin = principal.getAttribute("login");
        if (githubId == null || githubLogin == null) {
            response.sendRedirect("/login?error");
            return;
        }

        AuthResponse tokens = authService.loginWithGitHub(githubId.longValue(), githubLogin);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // only needed for the OAuth2 handshake
        }

        response.sendRedirect(frontendUrl + "/auth/callback#"
                + "accessToken=" + enc(tokens.accessToken())
                + "&refreshToken=" + enc(tokens.refreshToken())
                + "&username=" + enc(tokens.username()));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
