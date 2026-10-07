package com.ciphervault.ciphervault.auth;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import com.ciphervault.ciphervault.logging.RequestContext;
import com.ciphervault.ciphervault.security.JwtService;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.security.LoginRateLimiterService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KeyManagementService keyManagementService;
    private final LoginRateLimiterService loginRateLimiterService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.ciphervault.ciphervault.activity.ActivityService activityService;

    public void setActivityService(com.ciphervault.ciphervault.activity.ActivityService activityService) {
        this.activityService = activityService;
    }

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

    // Register a new user and initialize the user's encrypted data key.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        long start = System.currentTimeMillis();
        String requestId = RequestContext.getRequestId();

        if (request == null) {
            return badRequest("Request body is required");
        }

        if (request.getName() == null || request.getName().trim().isBlank()) {
            return badRequest("Name is required");
        }

        String name = request.getName().trim();
        if (name.length() > 255) {
            return badRequest("Name must not exceed 255 characters");
        }

        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());

        if (username == null) {
            return badRequest("Username is required");
        }

        if (email == null) {
            return badRequest("Email is required");
        }

        ResponseEntity<Map<String, String>> passwordError = validatePasswordStrength(request.getPassword());
        if (passwordError != null) {
            return passwordError;
        }

        if (userRepository.existsByUsername(username)) {
            ConsoleLogger.logAuthRegisterTrace(
                    requestId,
                    username,
                    email,
                    true,
                    false,
                    false,
                    "Username already exists",
                    elapsed(start));

            return badRequest("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            ConsoleLogger.logAuthRegisterTrace(
                    requestId,
                    username,
                    email,
                    false,
                    true,
                    false,
                    "Email already exists",
                    elapsed(start));

            return badRequest("Email already exists");
        }

        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserKey(keyManagementService.generateAndEncryptUserKey());

        userRepository.save(user);


        ConsoleLogger.logAuthRegisterTrace(
                requestId,
                user.getUsername(),
                user.getEmail(),
                false,
                false,
                true,
                null,
                elapsed(start));

        return ResponseEntity.ok(
                new RegisterResponse(
                        "User registered successfully",
                        user.getUsername(),
                        user.getEmail()));
    }

    // Authenticate the user and return a JWT token.
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        long start = System.currentTimeMillis();
        String requestId = RequestContext.getRequestId();

        if (request == null) {
            return badRequest("Request body is required");
        }

        String email = normalizeEmail(request.getEmail());

        if (email == null) {
            return badRequest("Email is required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return badRequest("Password is required");
        }

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = forwardedFor != null && !forwardedFor.isBlank() 
                ? forwardedFor.split(",")[0].trim() 
                : httpRequest.getRemoteAddr();

        if (loginRateLimiterService.isBlocked(clientIp, email)) {
            long seconds =
                    loginRateLimiterService.getRemainingLockoutSeconds(clientIp, email);
            long minutes = Math.max(1, (seconds + 59) / 60);

            ConsoleLogger.logAuthLoginTrace(
                    requestId,
                    email,
                    null,
                    false,
                    false,
                    "Rate limited",
                    elapsed(start));

            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(
                            "Retry-After",
                            String.valueOf(seconds > 0 ? seconds : 900))
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "message",
                                    "Too many failed login attempts. Please wait "
                                            + minutes
                                            + " minute(s) before trying again."));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword())) {
            ConsoleLogger.logAuthLoginTrace(
                    requestId,
                    email,
                    user != null ? user.getId() : null,
                    user != null,
                    false,
                    "Invalid credentials",
                    elapsed(start));

            loginRateLimiterService.recordFailedAttempt(clientIp, email);

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        loginRateLimiterService.recordSuccessfulLogin(clientIp, email);

        // Generate a missing key for accounts created before user-key support existed.
        if (user.getUserKey() == null) {
            keyManagementService.getOrGenerateUserKey(user);
        }

        int tokenVersion = user.getTokenVersion() != null
                ? user.getTokenVersion()
                : 1;

        String token = jwtService.generateToken(user.getEmail(), tokenVersion);


        ConsoleLogger.logAuthLoginTrace(
                requestId,
                user.getEmail(),
                user.getId(),
                true,
                true,
                null,
                elapsed(start));

        if (activityService != null) {
            try {
                activityService.logEvent(
                        user,
                        com.ciphervault.ciphervault.activity.EventType.SIGN_IN,
                        "User signed in",
                        1,
                        0L,
                        null,
                        "SUCCESS",
                        null);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                new LoginResponse(
                        true,
                        "Login successful",
                        token,
                        user.getUsername(),
                        user.getName()));
    }

    // Change the authenticated user's password and invalidate previously issued tokens.
    @RequestMapping(
            value = "/change-password",
            method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        long start = System.currentTimeMillis();

        if (authentication == null || !authentication.isAuthenticated()) {
            return unauthorized("Authentication required");
        }

        if (request == null) {
            return badRequest("Request body is required");
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "message",
                                    "User not found"));
        }

        if (request.getCurrentPassword() == null
                || request.getCurrentPassword().isBlank()) {
            return badRequest("Current password is required");
        }

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {
            return badRequest("Incorrect current password");
        }

        if (request.getNewPassword() == null
                || request.getNewPassword().isBlank()) {
            return badRequest("New password is required");
        }

        ResponseEntity<Map<String, String>> passwordError = validatePasswordStrength(request.getNewPassword());
        if (passwordError != null) {
            return passwordError;
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            return badRequest(
                    "New password cannot be the same as current password");
        }

        if (request.getConfirmPassword() == null
                || !request.getNewPassword().equals(request.getConfirmPassword())) {
            return badRequest(
                    "New password and confirmation do not match");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        int tokenVersion = user.getTokenVersion() != null
                ? user.getTokenVersion()
                : 1;

        user.setTokenVersion(tokenVersion + 1);
        userRepository.save(user);

        ConsoleLogger.logAuthPasswordChanged(
                RequestContext.getRequestId(),
                user.getEmail(),
                elapsed(start));

        // Generate a fresh token because the password change invalidates previous tokens.
        String newToken = jwtService.generateToken(
                user.getEmail(),
                user.getTokenVersion());

        if (activityService != null) {
            try {
                activityService.logEvent(
                        user,
                        com.ciphervault.ciphervault.activity.EventType.PASSWORD_CHANGED,
                        "Password changed successfully",
                        1,
                        0L,
                        null,
                        "SUCCESS",
                        null);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,
                        "message",
                        "Password changed successfully",
                        "token",
                        newToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 1;
                user.setTokenVersion(tokenVersion + 1);
                userRepository.save(user);

                if (activityService != null) {
                    try {
                        activityService.logEvent(
                                user,
                                com.ciphervault.ciphervault.activity.EventType.SIGN_OUT,
                                "User logged out",
                                1,
                                0L,
                                null,
                                "SUCCESS",
                                null);
                    } catch (Exception ignored) {}
                }
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    // Normalize usernames before validation and database lookups.
    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return username.trim();
    }

    // Normalize email addresses so registration and login use the same representation.
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    // Create a consistent bad-request response.
    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", message));
    }

    // Create a consistent unauthorized response.
    private ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        Map.of(
                                "success",
                                false,
                                "message",
                                message));
    }

    // Validate password strength according to security policy.
    private ResponseEntity<Map<String, String>> validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return badRequest("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            return badRequest("Password must contain at least one uppercase character");
        }
        if (!password.matches(".*[a-z].*")) {
            return badRequest("Password must contain at least one lowercase character");
        }
        if (!password.matches(".*\\d.*")) {
            return badRequest("Password must contain at least one number");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return badRequest("Password must contain at least one special symbol");
        }
        return null;
    }

    // Calculate how long the current request took.
    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    // Represent the registration request received from the Android client.
    public static class RegisterRequest {

        private String name;
        private String username;
        private String email;
        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

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

    // Represent the successful registration response.
    public record RegisterResponse(
            String message,
            String username,
            String email) {
    }

    // Represent the login request received from the Android client.
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

    // Represent the successful login response containing the JWT.
    public record LoginResponse(
            boolean success,
            String message,
            String token,
            String username,
            String name) {
    }

    // Represent the password-change request received from the Android client.
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
