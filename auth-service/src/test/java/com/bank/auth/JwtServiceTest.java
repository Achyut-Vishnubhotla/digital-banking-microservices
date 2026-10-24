package com.bank.auth;

import com.bank.auth.model.User;
import com.bank.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters-long";
    private final JwtService jwt = new JwtService(SECRET, 15, 7);
    private final User alice = new User("alice", "alice@example.com", "hash", "USER");

    @Test
    void accessTokenRoundTrip() {
        Claims claims = jwt.parse(jwt.createAccessToken(alice));
        assertEquals("alice", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void refreshTokenIsMarkedAsRefresh() {
        Claims claims = jwt.parse(jwt.createRefreshToken(alice));
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.createAccessToken(alice);
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> jwt.parse(tampered));
    }
}
