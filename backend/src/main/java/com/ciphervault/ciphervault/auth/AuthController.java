package com.ciphervault.ciphervault.auth;

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

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // Validate username
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Username is required");
        }

        // Validate email
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        // Validate password
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Password is required");
        }

        // Check duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Email already exists");
        }

        // Create new user
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Store password as BCrypt hash
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Save user
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

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
}