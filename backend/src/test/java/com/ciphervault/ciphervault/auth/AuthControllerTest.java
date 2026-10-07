package com.ciphervault.ciphervault.auth;

import com.ciphervault.ciphervault.security.JwtService;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.security.LoginRateLimiterService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private KeyManagementService keyManagementService;
    private LoginRateLimiterService loginRateLimiterService;
    private AuthController authController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);
        keyManagementService = Mockito.mock(KeyManagementService.class);
        loginRateLimiterService = Mockito.mock(LoginRateLimiterService.class);
        authentication = Mockito.mock(Authentication.class);
        authController = new AuthController(userRepository, passwordEncoder, jwtService, keyManagementService, loginRateLimiterService);
    }

    @Test
    void registerShouldReturnJsonRegisterResponse() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("Alice Cooper");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password123!");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(keyManagementService.generateAndEncryptUserKey()).thenReturn("mock-encrypted-key");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(AuthController.RegisterResponse.class, response.getBody());

        AuthController.RegisterResponse body = (AuthController.RegisterResponse) response.getBody();
        assertEquals("User registered successfully", body.message());
        assertEquals("alice", body.username());
        assertEquals("alice@example.com", body.email());
        verify(userRepository).save(argThat(u -> "Alice Cooper".equals(u.getName()) && "alice".equals(u.getUsername())));
    }

    @Test
    void loginShouldReturnLoginResponseWithUsername() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("Password123!");

        User user = new User();
        user.setName("Alice Cooper");
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("hashedPassword");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(eq("alice@example.com"), anyInt())).thenReturn("mock-jwt-token");

        ResponseEntity<?> response = authController.login(req, Mockito.mock(jakarta.servlet.http.HttpServletRequest.class));

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(AuthController.LoginResponse.class, response.getBody());

        AuthController.LoginResponse body = (AuthController.LoginResponse) response.getBody();
        assertTrue(body.success());
        assertEquals("Login successful", body.message());
        assertEquals("mock-jwt-token", body.token());
        assertEquals("alice", body.username());
        assertEquals("Alice Cooper", body.name());
    }

    @Test
    void changePasswordShouldSucceedWithValidNewPassword() {
        AuthController.ChangePasswordRequest req = new AuthController.ChangePasswordRequest();
        req.setCurrentPassword("OldPassword123!");
        req.setNewPassword("NewPassword456!");
        req.setConfirmPassword("NewPassword456!");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashedOldPassword");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123!", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword456!")).thenReturn("hashedNewPassword");
        when(jwtService.generateToken(eq("alice@example.com"), anyInt())).thenReturn("mock-new-token");

        ResponseEntity<?> response = authController.changePassword(req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals("Password changed successfully", body.get("message"));
        assertEquals(2, user.getTokenVersion());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void changePasswordShouldFailWhenCurrentPasswordMissing() {
        AuthController.ChangePasswordRequest req = new AuthController.ChangePasswordRequest();
        req.setNewPassword("NewPassword456!");
        req.setConfirmPassword("NewPassword456!");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashedOldPassword");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.changePassword(req, authentication);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Current password is required", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordShouldFailWithIncorrectCurrentPassword() {
        AuthController.ChangePasswordRequest req = new AuthController.ChangePasswordRequest();
        req.setCurrentPassword("WrongPassword!");
        req.setNewPassword("NewPassword456!");
        req.setConfirmPassword("NewPassword456!");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashedOldPassword");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashedOldPassword")).thenReturn(false);

        ResponseEntity<?> response = authController.changePassword(req, authentication);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Incorrect current password", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordShouldFailWithMismatchedConfirmation() {
        AuthController.ChangePasswordRequest req = new AuthController.ChangePasswordRequest();
        req.setCurrentPassword("OldPassword123!");
        req.setNewPassword("NewPassword456!");
        req.setConfirmPassword("DifferentPassword789!");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashedOldPassword");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123!", "hashedOldPassword")).thenReturn(true);

        ResponseEntity<?> response = authController.changePassword(req, authentication);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("New password and confirmation do not match", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordShouldFailWhenPasswordTooShort() {
        AuthController.ChangePasswordRequest req = new AuthController.ChangePasswordRequest();
        req.setCurrentPassword("OldPassword123!");
        req.setNewPassword("1234567");
        req.setConfirmPassword("1234567");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashedOldPassword");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123!", "hashedOldPassword")).thenReturn(true);

        ResponseEntity<?> response = authController.changePassword(req, authentication);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Password must be at least 8 characters", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldFailWhenNameIsBlank() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("   ");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password123!");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Name is required", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldFailWhenPasswordTooShort() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("Alice");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Pass1!"); // 6 chars, under 8

        ResponseEntity<?> response = authController.register(req);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Password must be at least 8 characters", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldFailWhenPasswordLacksUppercase() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("Alice");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123!");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Password must contain at least one uppercase character", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldFailWhenPasswordLacksLowercase() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("Alice");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("PASSWORD123!");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Password must contain at least one lowercase character", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldFailWhenPasswordLacksNumber() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("Alice");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password!!!!");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Password must contain at least one number", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldFailWhenPasswordLacksSpecialSymbol() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setName("Alice");
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password123");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Password must contain at least one special symbol", body.get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldBlockAndReturn429WhenRateLimited() {
        com.ciphervault.ciphervault.security.LoginRateLimiterService rateLimiter =
                Mockito.mock(com.ciphervault.ciphervault.security.LoginRateLimiterService.class);
        AuthController rateLimitedController = new AuthController(
                userRepository, passwordEncoder, jwtService, keyManagementService, rateLimiter
        );

        when(rateLimiter.isBlocked(any(), eq("blocked@example.com"))).thenReturn(true);
        when(rateLimiter.getRemainingLockoutSeconds(any(), eq("blocked@example.com"))).thenReturn(600L);

        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setEmail("blocked@example.com");
        req.setPassword("AnyPassword123!");

        ResponseEntity<?> response = rateLimitedController.login(req, Mockito.mock(jakarta.servlet.http.HttpServletRequest.class));

        assertEquals(429, response.getStatusCode().value());
        assertNotNull(response.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void logoutShouldIncrementTokenVersionAndReturnSuccess() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setTokenVersion(1);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.logout(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals("Logged out successfully", body.get("message"));
        assertEquals(2, user.getTokenVersion());
        verify(userRepository, times(1)).save(user);
    }
}
