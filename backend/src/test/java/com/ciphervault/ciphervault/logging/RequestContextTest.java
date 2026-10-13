package com.ciphervault.ciphervault.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void testDefaultInitializationGeneratesRequestId() {
        RequestContext.RequestInfo info = RequestContext.init();
        assertNotNull(info);
        assertNotNull(info.getRequestId());
        assertTrue(info.getRequestId().startsWith("REQ-"));
        assertEquals(info.getRequestId(), RequestContext.getRequestId());
        assertEquals(info.getRequestId(), MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
        assertEquals(info.getRequestId(), MDC.get(RequestContext.MDC_KEY_CORRELATION_ID));
    }

    @Test
    void testValidClientCorrelationIdPreserved() {
        String clientVal = "CLIENT-TRACE-999";
        RequestContext.RequestInfo info = RequestContext.init(clientVal);
        assertEquals(clientVal, info.getRequestId());
        assertEquals(clientVal, RequestContext.getRequestId());
        assertEquals(clientVal, MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
    }

    @Test
    void testInvalidClientCorrelationIdWithNewlinesReplaced() {
        // Attempt log injection with newline character
        String malicious = "TRACE-123\n[ERROR] Injected fake log";
        RequestContext.RequestInfo info = RequestContext.init(malicious);
        assertNotEquals(malicious, info.getRequestId());
        assertTrue(info.getRequestId().startsWith("REQ-"));
        assertEquals(info.getRequestId(), MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
    }

    @Test
    void testClearRemovesThreadLocalAndMdc() {
        RequestContext.init("REQ-CLEAR-TEST");
        assertEquals("REQ-CLEAR-TEST", RequestContext.getRequestId());
        assertNotNull(MDC.get(RequestContext.MDC_KEY_REQUEST_ID));

        RequestContext.clear();
        assertEquals("SYS", RequestContext.getRequestId());
        assertNull(MDC.get(RequestContext.MDC_KEY_REQUEST_ID));
        assertNull(MDC.get(RequestContext.MDC_KEY_CORRELATION_ID));
    }
}
