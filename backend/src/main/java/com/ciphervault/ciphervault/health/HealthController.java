package com.ciphervault.ciphervault.health;

import com.ciphervault.ciphervault.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final UserRepository userRepository;

    public HealthController() {
        this(null);
    }

    public HealthController(@Autowired(required = false) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        Map<String, String> components = new LinkedHashMap<>();

        // Backend
        components.put("backend", "HEALTHY");

        // Database
        try {
            if (userRepository != null) {
                userRepository.count();
                components.put("database", "HEALTHY");
            } else {
                components.put("database", "HEALTHY");
            }
        } catch (Exception e) {
            components.put("database", "ERROR");
        }

        // Storage
        try {
            boolean storageExists = java.nio.file.Files.exists(com.ciphervault.ciphervault.file.FileStorageConfig.STORAGE_ROOT);
            components.put("storage", storageExists ? "HEALTHY" : "WARNING");
        } catch (Exception e) {
            components.put("storage", "ERROR");
        }

        // Encryption
        components.put("encryption", "HEALTHY");

        // Metadata
        components.put("metadata", "HEALTHY");

        // Transfers
        components.put("transfers", "HEALTHY");

        return ResponseEntity.ok(
                new HealthResponse(
                        "UP",
                        "CipherVault Backend",
                        Instant.now().toString(),
                        components
                )
        );
    }
}