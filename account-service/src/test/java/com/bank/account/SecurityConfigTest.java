package com.bank.account;

import com.bank.account.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters-long";
    private final JwtDecoder decoder = new SecurityConfig().jwtDecoder(SECRET);

    private static String token(String type, String secret, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("alice")
                .claim("role", "USER")
                .claim("type", type)
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void acceptsValidAccessToken() {
        Jwt jwt = decoder.decode(token("access", SECRET, 60_000));
        assertEquals("alice", jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("role"));
    }

    @Test
    void rejectsRefreshTokenUsedAsAccessToken() {
        assertThrows(JwtException.class, () -> decoder.decode(token("refresh", SECRET, 60_000)));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String other = "a-completely-different-secret-with-32-plus-chars!";
        assertThrows(JwtException.class, () -> decoder.decode(token("access", other, 60_000)));
    }

    @Test
    void rejectsExpiredToken() {
        // Nimbus allows 60s clock skew by default, so expire it well in the past.
        assertThrows(JwtException.class, () -> decoder.decode(token("access", SECRET, -300_000)));
    }
}
