package com.ciphervault.app.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ciphervault.app.auth.model.HealthResponse;

import org.junit.Test;

import java.util.Collections;

/**
 * Unit tests verifying URL normalization, URL validation, and health check interpretation (Section 43).
 */
public class ServerConnectionPreferencesTest {

    @Test
    public void testUrlNormalization() {
        // Adds missing http scheme and trailing slash
        assertEquals("http://10.0.2.2:8080/", ServerConnectionPreferences.normalizeUrl("10.0.2.2:8080"));

        // Single trailing slash enforcement
        assertEquals("http://10.0.2.2:8080/", ServerConnectionPreferences.normalizeUrl("http://10.0.2.2:8080"));
        assertEquals("http://10.0.2.2:8080/", ServerConnectionPreferences.normalizeUrl("http://10.0.2.2:8080/"));
        assertEquals("http://10.0.2.2:8080/", ServerConnectionPreferences.normalizeUrl("http://10.0.2.2:8080///"));

        // HTTPS scheme preserved
        assertEquals("https://api.ciphervault.com/", ServerConnectionPreferences.normalizeUrl("https://api.ciphervault.com"));
        assertEquals("https://api.ciphervault.com/", ServerConnectionPreferences.normalizeUrl("https://api.ciphervault.com/"));

        // Trims whitespace
        assertEquals("http://127.0.0.1:8080/", ServerConnectionPreferences.normalizeUrl("   http://127.0.0.1:8080   "));

        // Empty / null fallback
        assertEquals(ServerConnectionPreferences.DEFAULT_SERVER_URL, ServerConnectionPreferences.normalizeUrl(""));
        assertEquals(ServerConnectionPreferences.DEFAULT_SERVER_URL, ServerConnectionPreferences.normalizeUrl("   "));
    }

    @Test
    public void testUrlValidation() {
        assertTrue(ServerConnectionPreferences.isValidUrl("http://10.0.2.2:8080/"));
        assertTrue(ServerConnectionPreferences.isValidUrl("http://127.0.0.1:8080/"));
        assertTrue(ServerConnectionPreferences.isValidUrl("https://api.ciphervault.com/"));

        assertFalse(ServerConnectionPreferences.isValidUrl(""));
        assertFalse(ServerConnectionPreferences.isValidUrl("   "));
        assertFalse(ServerConnectionPreferences.isValidUrl(null));
    }

    @Test
    public void testHealthResponseInterpretation() {
        HealthResponse upResponse = new HealthResponse("UP", "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        assertTrue("status == UP must evaluate to isUp() true", upResponse.isUp());

        HealthResponse upCaseInsensitive = new HealthResponse("up", "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        assertTrue("status == 'up' case-insensitive must evaluate to isUp() true", upCaseInsensitive.isUp());

        HealthResponse downResponse = new HealthResponse("DOWN", "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        assertFalse("status == DOWN must evaluate to isUp() false", downResponse.isUp());

        HealthResponse nullStatus = new HealthResponse(null, "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        assertFalse("null status must evaluate to isUp() false", nullStatus.isUp());
    }
}
