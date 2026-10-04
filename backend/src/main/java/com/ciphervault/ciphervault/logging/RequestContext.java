package com.ciphervault.ciphervault.logging;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-local context for correlating all log statements belonging to the same HTTP request.
 * Monotonically increments request IDs (REQ-0001, REQ-0002, etc.).
 */
public final class RequestContext {

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);
    private static final ThreadLocal<RequestInfo> CURRENT_REQUEST = new ThreadLocal<>();

    private RequestContext() {
    }

    public static class RequestInfo {
        private final String requestId;
        private final String method;
        private final String uri;
        private final long startTime;
        private String user;
        private Long userId;

        public RequestInfo(String requestId, String method, String uri) {
            this.requestId = requestId;
            this.method = method;
            this.uri = uri;
            this.startTime = System.currentTimeMillis();
        }

        public String getRequestId() {
            return requestId;
        }

        public String getMethod() {
            return method;
        }

        public String getUri() {
            return uri;
        }

        public long getStartTime() {
            return startTime;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    /**
     * Initializes request context for a new incoming request.
     */
    public static RequestInfo init(String method, String uri) {
        long id = REQUEST_COUNTER.incrementAndGet();
        String reqId = String.format("REQ-%04d", id);
        RequestInfo info = new RequestInfo(reqId, method, uri);
        CURRENT_REQUEST.set(info);
        return info;
    }

    /**
     * Returns current request context or null if none exists.
     */
    public static RequestInfo get() {
        return CURRENT_REQUEST.get();
    }

    /**
     * Returns the current request ID (e.g. REQ-0042) or "SYS" if running outside an HTTP request.
     */
    public static String getRequestId() {
        RequestInfo info = CURRENT_REQUEST.get();
        return info != null ? info.getRequestId() : "SYS";
    }

    public static void setUser(String user, Long userId) {
        RequestInfo info = CURRENT_REQUEST.get();
        if (info != null) {
            info.setUser(user);
            info.setUserId(userId);
        }
    }

    public static String getUser() {
        RequestInfo info = CURRENT_REQUEST.get();
        return info != null ? info.getUser() : null;
    }

    public static Long getUserId() {
        RequestInfo info = CURRENT_REQUEST.get();
        return info != null ? info.getUserId() : null;
    }

    public static void clear() {
        CURRENT_REQUEST.remove();
    }
}
