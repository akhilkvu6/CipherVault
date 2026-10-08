package com.ciphervault.app.auth.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Unit tests verifying error parsing, 401 classification, 429 Retry-After, and IOException mapping (Section 43).
 */
public class AuthErrorParserTest {

    @Test
    public void test400MessageExtraction() {
        String jsonError = "{\"message\":\"Username already exists\"}";
        ResponseBody body = ResponseBody.create(MediaType.get("application/json"), jsonError);
        Response<Object> response = Response.error(400, body);

        String message = AuthErrorParser.parseError(response);
        assertEquals("Username already exists", message);
    }

    @Test
    public void test401InvalidCredentialsMessage() {
        String jsonError = "{\"message\":\"Bad credentials\"}";
        ResponseBody body = ResponseBody.create(MediaType.get("application/json"), jsonError);
        Response<Object> response = Response.error(401, body);

        String message = AuthErrorParser.parseError(response);
        assertEquals("Bad credentials", message);

        // Fallback when error body is empty
        ResponseBody emptyBody = ResponseBody.create(MediaType.get("application/json"), "");
        Response<Object> emptyResponse = Response.error(401, emptyBody);
        assertEquals("Invalid email or password", AuthErrorParser.parseError(emptyResponse));
    }

    @Test
    public void test429RetryAfterParsing() {
        Headers headers = new Headers.Builder().add("Retry-After", "900").build();
        ResponseBody body = ResponseBody.create(MediaType.get("application/json"), "{\"message\":\"Locked\"}");
        okhttp3.Response rawResponse = new okhttp3.Response.Builder()
                .code(429)
                .message("Too Many Requests")
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .request(new okhttp3.Request.Builder().url("http://localhost/api/auth/login").build())
                .headers(headers)
                .build();
        Response<Object> response = Response.error(body, rawResponse);

        assertEquals(900, AuthErrorParser.parseRetryAfter(response));
        String errorMsg = AuthErrorParser.parseError(response);
        assertTrue(errorMsg.contains("900 seconds"));
    }

    @Test
    public void test429DefaultFallbackWhenHeaderMissing() {
        ResponseBody body = ResponseBody.create(MediaType.get("application/json"), "{\"message\":\"Too many requests\"}");
        Response<Object> response = Response.error(429, body);

        // Fallback must be reasonable 60 seconds, not permanent/arbitrary 900
        assertEquals(60, AuthErrorParser.parseRetryAfter(response));
        String errorMsg = AuthErrorParser.parseError(response);
        assertTrue(errorMsg.contains("60 seconds"));
    }

    @Test
    public void testIOExceptionMapping() {
        IOException networkException = new IOException("Failed to connect to /10.0.2.2:8080");
        String message = AuthErrorParser.parseException(networkException);
        assertEquals("Cannot connect to server. Check your connection URL and network.", message);

        Exception genericException = new RuntimeException("NullPointerException");
        String genericMsg = AuthErrorParser.parseException(genericException);
        assertEquals("An unexpected error occurred. Please try again.", genericMsg);
    }
}
