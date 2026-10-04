package com.ciphervault.ciphervault.health;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    @Test
    void healthShouldReturnUpStatusWithServiceNameAndTimestamp() {
        HealthController controller = new HealthController();
        ResponseEntity<HealthResponse> response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().getStatus());
        assertEquals("CipherVault Backend", response.getBody().getService());
        assertNotNull(response.getBody().getTimestamp());
    }
}
