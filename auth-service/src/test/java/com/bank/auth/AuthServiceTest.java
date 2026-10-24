package com.bank.auth;

import com.bank.auth.dto.AuthDtos.AuthResponse;
import com.bank.auth.dto.AuthDtos.LoginRequest;
import com.bank.auth.dto.AuthDtos.RegisterRequest;
import com.bank.auth.model.User;
import com.bank.auth.repo.UserRepository;
import com.bank.auth.security.JwtService;
import com.bank.auth.service.AuthService;
import com.bank.auth.service.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwt = new JwtService("unit-test-secret-that-is-at-least-32-characters-long", 15, 7);

    private AuthService service() {
        return new AuthService(users, encoder, jwt);
    }

    @Test
    void registerStoresHashedPasswordAndUserRole() {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.existsByEmail("alice@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse res = service().register(new RegisterRequest("alice", "alice@example.com", "s3cretPass"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertNotEquals("s3cretPass", saved.getValue().getPasswordHash());
        assertTrue(encoder.matches("s3cretPass", saved.getValue().getPasswordHash()));
        assertEquals("USER", saved.getValue().getRole());
        assertNotNull(res.accessToken());
        assertNotNull(res.refreshToken());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(users.existsByUsername("alice")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service().register(new RegisterRequest("alice", "a@example.com", "s3cretPass")));
        verify(users, never()).save(any());
    }

    @Test
    void loginFailsWithWrongPassword() {
        User alice = new User("alice", "a@example.com", encoder.encode("right-password"), "USER");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        assertThrows(BadCredentialsException.class,
                () -> service().login(new LoginRequest("alice", "wrong-password")));
    }

    @Test
    void refreshRejectsAccessToken() {
        User alice = new User("alice", "a@example.com", "hash", "USER");
        String accessToken = jwt.createAccessToken(alice);
        assertThrows(BadCredentialsException.class, () -> service().refresh(accessToken));
    }

    @Test
    void registerRejectsReservedGitHubPrefix() {
        assertThrows(ConflictException.class,
                () -> service().register(new RegisterRequest("gh_octocat", "o@example.com", "s3cretPass")));
        verify(users, never()).save(any());
    }

    @Test
    void githubLoginCreatesLocalUserOnFirstSignIn() {
        when(users.findByUsername("gh_octocat")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse res = service().loginWithGitHub(583231L, "octocat");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("gh_octocat", saved.getValue().getUsername());
        assertEquals("583231+octocat@users.noreply.github.com", saved.getValue().getEmail());
        assertEquals("USER", saved.getValue().getRole());
        assertEquals("gh_octocat", res.username());
        assertNotNull(res.accessToken());
    }

    @Test
    void githubLoginReusesExistingUser() {
        User existing = new User("gh_octocat", "583231+octocat@users.noreply.github.com", "hash", "USER");
        when(users.findByUsername("gh_octocat")).thenReturn(Optional.of(existing));

        AuthResponse res = service().loginWithGitHub(583231L, "octocat");

        verify(users, never()).save(any());
        assertEquals("gh_octocat", res.username());
    }
}
