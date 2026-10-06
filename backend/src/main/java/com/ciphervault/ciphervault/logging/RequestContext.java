package com.ciphervault.ciphervault.logging;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-local context for correlating log statements belonging to the same HTTP request.
 * Monotonically increments request IDs (REQ-0001, REQ-0002, etc.).
 */
public final class RequestContext {

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);
    private static final ThreadLocal<RequestInfo> CURRENT_REQUEST = new ThreadLocal<>();

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
     * Initializes request context for a new incoming request.
     */
    public static RequestInfo init() {
        long id = REQUEST_COUNTER.incrementAndGet();
        RequestInfo info = new RequestInfo(String.format("REQ-%04d", id));
        CURRENT_REQUEST.set(info);
        return info;
    }

    /**
     * Returns the current request ID or "SYS" if running outside an HTTP request.
     */
    public static String getRequestId() {
        RequestInfo info = CURRENT_REQUEST.get();
        return info != null ? info.getRequestId() : "SYS";
    }

    /**
     * Clears the request context after request processing.
     */
    public static void clear() {
        CURRENT_REQUEST.remove();
    }
}