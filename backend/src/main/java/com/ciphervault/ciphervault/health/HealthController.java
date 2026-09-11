package com.ciphervault.ciphervault.health;

import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    public HealthController() {
        ConsoleLogger.success("HealthController initialized successfully.");
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {

        ConsoleLogger.info("Health check requested.");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "CipherVault Backend");
        response.put("timestamp", Instant.now().toString());

        ConsoleLogger.success("Health check completed successfully.");

        return ResponseEntity.ok(response);
    }
}