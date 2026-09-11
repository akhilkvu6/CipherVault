package com.ciphervault.ciphervault.auth;

import com.ciphervault.ciphervault.security.JwtService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
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
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        // Validate username
        if (request.getUsername() == null
                || request.getUsername().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Username is required");
        }

        // Validate email
        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        // Validate password
        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Password is required");
        }

        // Check duplicate username
        if (userRepository.existsByUsername(
                request.getUsername())) {

            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(
                request.getEmail())) {

            return ResponseEntity.badRequest()
                    .body("Email already exists");
        }

        // Create new user
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Hash password using BCrypt
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Save user
        userRepository.save(user);

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

        // Validate email
        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        // Validate password
        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Password is required");
        }

        // Find user by email
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        // Email is not registered
        if (user == null) {

            return ResponseEntity.status(401)
                    .body("Email is not registered");
        }

        // Verify password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            return ResponseEntity.status(401)
                    .body("Wrong password");
        }

        // Generate JWT
        String token = jwtService.generateToken(
                user.getEmail()
        );

        // Return successful login response
        LoginResponse response = new LoginResponse(
                true,
                "Login successful",
                token
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