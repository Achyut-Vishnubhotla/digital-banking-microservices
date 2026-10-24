package com.bank.auth.service;

import com.bank.auth.dto.AuthDtos.AuthResponse;
import com.bank.auth.dto.AuthDtos.LoginRequest;
import com.bank.auth.dto.AuthDtos.RegisterRequest;
import com.bank.auth.model.User;
import com.bank.auth.repo.UserRepository;
import com.bank.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Usernames starting with this prefix belong to GitHub sign-ins and cannot be registered by hand. */
    public static final String GITHUB_PREFIX = "gh_";

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Prevents "pre-hijacking": someone registering gh_<victim> with a password they know.
        if (req.username().startsWith(GITHUB_PREFIX)) {
            throw new ConflictException("Usernames starting with '" + GITHUB_PREFIX + "' are reserved");
        }
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken");
        }
        if (users.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered");
        }
        // Role is always USER: nobody can self-register as ADMIN.
        User user = users.save(new User(req.username(), req.email(), encoder.encode(req.password()), "USER"));
        return tokensFor(user);
    }

    /**
     * Called after a successful GitHub OAuth2 login. The local user is keyed by the stable GitHub id/login.
     * We deliberately do NOT link to an existing local account by e-mail (unverified e-mails enable account takeover).
     * The password is random and never shown, so these users can only sign in through GitHub.
     */
    @Transactional
    public AuthResponse loginWithGitHub(long githubId, String githubLogin) {
        String username = GITHUB_PREFIX + githubLogin;
        User user = users.findByUsername(username).orElseGet(() -> users.save(new User(
                username,
                githubId + "+" + githubLogin + "@users.noreply.github.com",
                encoder.encode(UUID.randomUUID().toString()),
                "USER")));
        return tokensFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return tokensFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwt.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = users.findByUsername(claims.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        return tokensFor(user);
    }

    private AuthResponse tokensFor(User user) {
        return new AuthResponse(jwt.createAccessToken(user), jwt.createRefreshToken(user),
                user.getUsername(), user.getRole());
    }
}
