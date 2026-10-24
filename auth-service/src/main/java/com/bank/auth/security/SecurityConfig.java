package com.bank.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, OAuth2LoginSuccessHandler oauth2SuccessHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // JSON API without cookies; the OAuth2 flow is protected by its "state" parameter
                // Default session policy on purpose: the OAuth2 authorization-code handshake needs a short-lived
                // session to remember the "state". The handler below invalidates it once we have issued our own JWTs.
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/**", "/actuator/health",
                                "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .anyRequest().denyAll())
                .oauth2Login(o -> o
                        .successHandler(oauth2SuccessHandler)
                        .failureUrl("/login?error"));
        return http.build();
    }
}
