package com.ciphervault.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiClientUnitTest {

    @Test
    public void testSanitizeAndValidateUrl_AdbLoopback() {
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl("127.0.0.1:8080"));
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl("http://127.0.0.1:8080"));
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl("http://127.0.0.1:8080/"));
    }

    @Test
    public void testSanitizeAndValidateUrl_NeverConvertsAdbToHttps() {
        // Even if HTTPS was mistakenly passed or prefixed, development loopback must remain HTTP
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl("https://127.0.0.1:8080"));
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl("https://127.0.0.1:8080/"));
        assertEquals("http://localhost:8080/", ApiClient.sanitizeAndValidateUrl("https://localhost:8080"));
    }

    @Test
    public void testSanitizeAndValidateUrl_LocalNetworkNeverForcesHttps() {
        // WSL Hyper-V or LAN addresses must not be forced to HTTPS
        assertEquals("http://172.17.48.1:8080/", ApiClient.sanitizeAndValidateUrl("172.17.48.1:8080"));
        assertEquals("http://172.17.48.1:8080/", ApiClient.sanitizeAndValidateUrl("https://172.17.48.1:8080"));
        assertEquals("http://192.168.43.1:8080/", ApiClient.sanitizeAndValidateUrl("192.168.43.1:8080"));
        assertEquals("http://10.0.2.2:8080/", ApiClient.sanitizeAndValidateUrl("10.0.2.2:8080"));
    }

    @Test
    public void testSanitizeAndValidateUrl_DefaultFallback() {
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl(null));
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl(""));
        assertEquals("http://127.0.0.1:8080/", ApiClient.sanitizeAndValidateUrl("   "));
    }
}
