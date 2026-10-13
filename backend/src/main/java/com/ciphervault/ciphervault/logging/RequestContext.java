package com.ciphervault.ciphervault.logging;

import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Thread-local context and SLF4J MDC bridge for correlating log statements belonging
 * to the same HTTP request across asynchronous boundaries and downstream components.
 */
public final class RequestContext {

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);
    private static final ThreadLocal<RequestInfo> CURRENT_REQUEST = new ThreadLocal<>();
    private static final Pattern VALID_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    public static final String MDC_KEY_REQUEST_ID = "requestId";
    public static final String MDC_KEY_CORRELATION_ID = "correlationId";

    private RequestContext() {
    }

    public static class RequestInfo {
        private final String requestId;
        private final long startTime;

        private RequestInfo(String requestId) {
            this.requestId = requestId;
            this.startTime = System.currentTimeMillis();
        }

        public String getRequestId() {
            return requestId;
        }

        public long getStartTime() {
            return startTime;
        }
    }

    /**
     * Initializes request context with a generated identifier (REQ-0001, etc.).
     */
    public static RequestInfo init() {
        return init(null);
    }

    /**
     * Initializes request context, safely validating any client-supplied correlation ID.
     * If invalid or absent, generates a safe internal identifier.
     */
    public static RequestInfo init(String clientCorrelationId) {
        String reqId;
        if (clientCorrelationId != null && !clientCorrelationId.isBlank()) {
            String trimmed = clientCorrelationId.trim();
            if (VALID_CORRELATION_ID_PATTERN.matcher(trimmed).matches()) {
                reqId = trimmed;
            } else {
                reqId = generateRequestId();
            }
        } else {
            reqId = generateRequestId();
        }

        RequestInfo info = new RequestInfo(reqId);
        CURRENT_REQUEST.set(info);
        MDC.put(MDC_KEY_REQUEST_ID, reqId);
        MDC.put(MDC_KEY_CORRELATION_ID, reqId);
        return info;
    }

    private static String generateRequestId() {
        long id = REQUEST_COUNTER.incrementAndGet();
        return String.format("REQ-%04d", id);
    }

    /**
     * Returns the current request ID or "SYS" if running outside an HTTP request.
     */
    public static String getRequestId() {
        RequestInfo info = CURRENT_REQUEST.get();
        if (info != null) {
            return info.getRequestId();
        }
        String mdcId = MDC.get(MDC_KEY_REQUEST_ID);
        return mdcId != null ? mdcId : "SYS";
    }

    /**
     * Clears both ThreadLocal and SLF4J MDC to prevent ID leakage across pooled threads.
     */
    public static void clear() {
        CURRENT_REQUEST.remove();
        MDC.remove(MDC_KEY_REQUEST_ID);
        MDC.remove(MDC_KEY_CORRELATION_ID);
    }
}