package com.ciphervault.ciphervault.auth;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import com.ciphervault.ciphervault.logging.RequestContext;
import com.ciphervault.ciphervault.security.JwtService;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.security.LoginRateLimiterService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KeyManagementService keyManagementService;
    private final LoginRateLimiterService loginRateLimiterService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            KeyManagementService keyManagementService,
            LoginRateLimiterService loginRateLimiterService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.keyManagementService = keyManagementService;
        this.loginRateLimiterService = loginRateLimiterService;
    }

    // Register a new user and create the user's encrypted data key.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        long start = System.currentTimeMillis();
        String requestId = RequestContext.getRequestId();

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return badRequest("Username is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return badRequest("Email is required");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return badRequest("Password must be at least 6 characters");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            ConsoleLogger.logAuthRegisterTrace(
                    requestId, request.getUsername(), request.getEmail(),
                    true, false, false, "Username already exists",
                    elapsed(start));
            return badRequest("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            ConsoleLogger.logAuthRegisterTrace(
                    requestId, request.getUsername(), request.getEmail(),
                    false, true, false, "Email already exists",
                    elapsed(start));
            return badRequest("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserKey(keyManagementService.generateAndEncryptUserKey());

        userRepository.save(user);
        RequestContext.setUser(user.getEmail(), user.getId());

        ConsoleLogger.logAuthRegisterTrace(
                requestId, user.getUsername(), user.getEmail(),
                false, false, true, null, elapsed(start));

        return ResponseEntity.ok(
                new RegisterResponse(
                        "User registered successfully",
                        user.getUsername(),
                        user.getEmail()));
    }

    // Authenticate the user and return a JWT.
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        long start = System.currentTimeMillis();
        String requestId = RequestContext.getRequestId();

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return badRequest("Email is required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return badRequest("Password is required");
        }

        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpRequest.getRemoteAddr();
        }

        String email = request.getEmail().trim().toLowerCase();

        if (loginRateLimiterService.isBlocked(clientIp, email)) {
            long seconds = loginRateLimiterService.getRemainingLockoutSeconds(clientIp, email);
            long minutes = Math.max(1, (seconds + 59) / 60);

            ConsoleLogger.logAuthLoginTrace(
                    requestId, email, null, false, false,
                    "Rate limited", elapsed(start));

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(seconds > 0 ? seconds : 900))
                    .body(Map.of(
                            "success", false,
                            "message", "Too many failed login attempts. Please wait "
                                    + minutes + " minute(s) before trying again."));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            ConsoleLogger.logAuthLoginTrace(
                    requestId,
                    email,
                    user != null ? user.getId() : null,
                    user != null,
                    false,
                    "Invalid credentials",
                    elapsed(start));

            loginRateLimiterService.recordFailedAttempt(clientIp, email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        loginRateLimiterService.recordSuccessfulLogin(clientIp, email);

        // Generate a missing user key for older accounts.
        if (user.getUserKey() == null) {
            keyManagementService.getOrGenerateUserKey(user);
        }

        int tokenVersion = user.getTokenVersion() != null
                ? user.getTokenVersion()
                : 1;

        String token = jwtService.generateToken(user.getEmail(), tokenVersion);

        RequestContext.setUser(user.getEmail(), user.getId());

        ConsoleLogger.logAuthLoginTrace(
                requestId,
                user.getEmail(),
                user.getId(),
                true,
                true,
                null,
                elapsed(start));

        return ResponseEntity.ok(
                new LoginResponse(
                        true,
                        "Login successful",
                        token,
                        user.getUsername()));
    }

    // Change the authenticated user's password and invalidate old tokens.
    @RequestMapping(
            value = "/change-password",
            method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "User not found"));
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            return badRequest("Current password is required");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return badRequest("Incorrect current password");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return badRequest("New password is required");
        }

        if (request.getNewPassword().length() < 6) {
            return badRequest("New password must be at least 6 characters");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            return badRequest("New password cannot be the same as current password");
        }

        if (request.getConfirmPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            return badRequest("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        int tokenVersion = user.getTokenVersion() != null
                ? user.getTokenVersion()
                : 1;

        user.setTokenVersion(tokenVersion + 1);
        userRepository.save(user);

        ConsoleLogger.logAuthPasswordChanged(
                RequestContext.getRequestId(),
                user.getEmail(),
                25);

        String newToken = jwtService.generateToken(
                user.getEmail(),
                user.getTokenVersion());

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Password changed successfully",
                        "token", newToken));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    // Registration request.
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public record RegisterResponse(
            String message,
            String username,
            String email) {
    }

    // Login request.
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public record LoginResponse(
            boolean success,
            String message,
            String token,
            String username) {
    }

    // Password-change request.
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;

        public ChangePasswordRequest() {
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}