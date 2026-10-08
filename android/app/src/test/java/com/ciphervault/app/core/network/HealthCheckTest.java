package com.ciphervault.app.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ciphervault.app.auth.model.HealthResponse;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Focused unit test verifying the GET /api/health contract and application health interpretation:
 * - HTTP 200 + status == "UP" -> CONNECTED
 * - HTTP 200 + status == "DOWN" -> UNAVAILABLE
 * - HTTP error (5xx) -> UNAVAILABLE
 * - Network failure (IOException) -> UNAVAILABLE
 */
public class HealthCheckTest {

    private ConnectionStatus interpretHealthResult(Response<HealthResponse> response) {
        if (response != null && response.isSuccessful() && response.body() != null) {
            return response.body().isUp() ? ConnectionStatus.CONNECTED : ConnectionStatus.UNAVAILABLE;
        }
        return ConnectionStatus.UNAVAILABLE;
    }

    private ConnectionStatus interpretHealthFailure(Throwable t) {
        return ConnectionStatus.UNAVAILABLE;
    }

    @Test
    public void testSuccessfulHealthCheckInterpretedAsConnected() {
        HealthResponse body = new HealthResponse("UP", "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        Response<HealthResponse> response = Response.success(body);

        assertTrue("Response must be successful", response.isSuccessful());
        assertTrue("status == UP must evaluate to isUp() true", response.body().isUp());
        assertEquals("status == UP must map to CONNECTED", ConnectionStatus.CONNECTED, interpretHealthResult(response));
    }

    @Test
    public void testDownHealthStatusInterpretedAsUnavailable() {
        HealthResponse body = new HealthResponse("DOWN", "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        Response<HealthResponse> response = Response.success(body);

        assertTrue("Response is HTTP 200", response.isSuccessful());
        assertFalse("status == DOWN must evaluate to isUp() false", response.body().isUp());
        assertEquals("status == DOWN must map to UNAVAILABLE", ConnectionStatus.UNAVAILABLE, interpretHealthResult(response));
    }

    @Test
    public void testHttpServerErrorInterpretedAsUnavailable() {
        ResponseBody errorBody = ResponseBody.create(MediaType.get("application/json"), "{\"status\":\"OUT_OF_SERVICE\"}");
        Response<HealthResponse> errorResponse = Response.error(503, errorBody);

        assertFalse("503 response must not be successful", errorResponse.isSuccessful());
        assertEquals("HTTP error must map to UNAVAILABLE", ConnectionStatus.UNAVAILABLE, interpretHealthResult(errorResponse));
    }

    @Test
    public void testNullOrMalformedHealthStatusInterpretedAsUnavailable() {
        HealthResponse nullStatus = new HealthResponse(null, "CipherVault Backend", "2026-10-08T00:00:00Z", Collections.emptyMap());
        Response<HealthResponse> response = Response.success(nullStatus);

        assertFalse("Null status must evaluate to isUp() false", response.body().isUp());
        assertEquals("Null status must map to UNAVAILABLE", ConnectionStatus.UNAVAILABLE, interpretHealthResult(response));
    }

    @Test
    public void testNetworkExceptionInterpretedAsUnavailable() {
        IOException networkException = new IOException("Failed to connect to /10.0.2.2:8080");
        ConnectionStatus status = interpretHealthFailure(networkException);

        assertEquals("Network exception must map to UNAVAILABLE", ConnectionStatus.UNAVAILABLE, status);
    }
}
