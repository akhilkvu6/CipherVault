package com.ciphervault.ciphervault.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void testFilterSetsCorrelationIdAndCleansMdc() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            // Verify MDC is active during request processing
            assertNotNull(MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
            assertEquals(response.getHeader("X-Correlation-ID"), MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
            return null;
        }).when(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        filter.doFilter(request, response, chain);

        // Verify response header is set
        String correlationHeader = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationHeader);
        assertTrue(correlationHeader.startsWith("REQ-"));

        // Verify MDC is completely cleared after filter completes
        assertNull(MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
        assertNull(MDC.get(RequestContext.MDC_KEY_CORRELATION_ID));
    }

    @Test
    void testClientSuppliedCorrelationHeaderRespected() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("X-Correlation-ID", "CLIENT-REQ-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        assertEquals("CLIENT-REQ-42", response.getHeader("X-Correlation-ID"));
        assertNull(MDC.get(RequestContext.MDC_KEY_REQUEST_ID)); // Cleared
    }

    @Test
    void testNonApiRequestsSkipped() {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/static/css/app.css");
        assertTrue(filter.shouldNotFilter(request));

        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/files");
        assertFalse(filter.shouldNotFilter(apiRequest));
    }
}
