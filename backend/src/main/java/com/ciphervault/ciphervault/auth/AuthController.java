package com.ciphervault.ciphervault.auth;

import com.ciphervault.ciphervault.security.JwtService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;

        ConsoleLogger.success(
                "AuthController initialized successfully."
        );
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        ConsoleLogger.info(
                "Registration request received."
        );

        // Validate username
        if (request.getUsername() == null
                || request.getUsername().isBlank()) {

            ConsoleLogger.warn(
                    "Registration failed: username is required."
            );

            return ResponseEntity.badRequest()
                    .body("Username is required");
        }

        // Validate email
        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            ConsoleLogger.warn(
                    "Registration failed: email is required."
            );

            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        // Validate password
        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            ConsoleLogger.warn(
                    "Registration failed: password is required."
            );

            return ResponseEntity.badRequest()
                    .body("Password is required");
        }

        // Check duplicate username
        if (userRepository.existsByUsername(
                request.getUsername())) {

            ConsoleLogger.warn(
                    "Registration failed: username already exists: "
                            + request.getUsername()
            );

            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(
                request.getEmail())) {

            ConsoleLogger.warn(
                    "Registration failed: email already exists: "
                            + request.getEmail()
            );

            return ResponseEntity.badRequest()
                    .body("Email already exists");
        }

        // Create new user
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        ConsoleLogger.info(
                "Hashing user password with BCrypt."
        );

        // Hash password using BCrypt
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        ConsoleLogger.success(
                "Password hashed successfully."
        );

        // Save user
        userRepository.save(user);

        ConsoleLogger.success(
                "User registered successfully: "
                        + user.getEmail()
        );

        return ResponseEntity.ok(
                "User registered successfully"
        );
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        ConsoleLogger.info(
                "Login request received: "
                        + request.getEmail()
        );

        // Validate email
        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            ConsoleLogger.warn(
                    "Login failed: email is required."
            );

            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        // Validate password
        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            ConsoleLogger.warn(
                    "Login failed: password is required."
            );

            return ResponseEntity.badRequest()
                    .body("Password is required");
        }

        // Find user by email
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        // Email is not registered
        if (user == null) {

            ConsoleLogger.warn(
                    "Login failed: email is not registered: "
                            + request.getEmail()
            );

            return ResponseEntity.status(401)
                    .body("Email is not registered");
        }

        ConsoleLogger.info(
                "Verifying user password."
        );

        // Verify password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            ConsoleLogger.warn(
                    "Login failed: incorrect password for: "
                            + request.getEmail()
            );

            return ResponseEntity.status(401)
                    .body("Wrong password");
        }

        ConsoleLogger.success(
                "Password verification successful: "
                        + user.getEmail()
        );

        // Generate JWT
        ConsoleLogger.info(
                "Generating JWT authentication token."
        );

        String token = jwtService.generateToken(
                user.getEmail()
        );

        ConsoleLogger.success(
                "JWT generated successfully for: "
                        + user.getEmail()
        );

        // Return successful login response
        LoginResponse response = new LoginResponse(
                true,
                "Login successful",
                token
        );

        ConsoleLogger.success(
                "Login successful: "
                        + user.getEmail()
        );

        return ResponseEntity.ok(response);
    }

    // =========================
    // REGISTER REQUEST
    // =========================

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

    // =========================
    // LOGIN REQUEST
    // =========================

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

    // =========================
    // LOGIN RESPONSE
    // =========================

    public static class LoginResponse {

        private boolean success;
        private String message;
        private String token;

        public LoginResponse(
                boolean success,
                String message,
                String token) {

            this.success = success;
            this.message = message;
            this.token = token;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getToken() {
            return token;
        }
    }
}