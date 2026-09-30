package com.ciphervault.ciphervault.auth;

import com.ciphervault.ciphervault.security.JwtService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);
        authController = new AuthController(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerShouldReturnJsonRegisterResponse() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password123!");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(AuthController.RegisterResponse.class, response.getBody());

        AuthController.RegisterResponse body = (AuthController.RegisterResponse) response.getBody();
        assertEquals("User registered successfully", body.getMessage());
        assertEquals("alice", body.getUsername());
        assertEquals("alice@example.com", body.getEmail());
    }

    @Test
    void loginShouldReturnLoginResponseWithUsername() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("Password123!");

        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("hashedPassword");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken("alice@example.com")).thenReturn("mock-jwt-token");

        ResponseEntity<?> response = authController.login(req);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(AuthController.LoginResponse.class, response.getBody());

        AuthController.LoginResponse body = (AuthController.LoginResponse) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Login successful", body.getMessage());
        assertEquals("mock-jwt-token", body.getToken());
        assertEquals("alice", body.getUsername());
    }
}
