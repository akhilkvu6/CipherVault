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
}