package com.ciphervault.ciphervault.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void shouldGenerateAndValidateToken() {

        JwtService jwtService = new JwtService();

        String email = "akhil@example.com";

        String token = jwtService.generateToken(email);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
                email,
                jwtService.extractEmail(token)
        );

        assertTrue(
                jwtService.isTokenValid(token, email)
        );
    }

    @Test
    void shouldRejectTokenForDifferentEmail() {

        JwtService jwtService = new JwtService();

        String token = jwtService.generateToken(
                "akhil@example.com"
        );

        assertFalse(
                jwtService.isTokenValid(
                        token,
                        "another@example.com"
                )
        );
    }

    @Test
    void shouldGenerateAndExtractTokenVersion() {
        JwtService jwtService = new JwtService();
        String email = "alice@example.com";

        String tokenDefault = jwtService.generateToken(email);
        assertEquals(Integer.valueOf(1), jwtService.extractTokenVersion(tokenDefault));

        String tokenV2 = jwtService.generateToken(email, 2);
        assertEquals(Integer.valueOf(2), jwtService.extractTokenVersion(tokenV2));
    }
}