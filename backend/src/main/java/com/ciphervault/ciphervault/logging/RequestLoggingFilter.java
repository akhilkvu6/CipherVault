package com.ciphervault.ciphervault.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that establishes correlation IDs via MDC and outputs concise,
 * colour-coded HTTP access logging conforming to the CipherVault Standard.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        // Check for client-supplied correlation headers
        String clientCorrId = request.getHeader("X-Correlation-ID");
        if (clientCorrId == null || clientCorrId.isBlank()) {
            clientCorrId = request.getHeader("X-Request-ID");
        }

        RequestContext.RequestInfo reqInfo = RequestContext.init(clientCorrId);
        String reqId = reqInfo.getRequestId();

        // Echo correlation ID back to the client
        response.setHeader("X-Correlation-ID", reqId);

        // Sanitize path for safe logging (redact secrets/tokens from query parameters)
        String safePath = sanitizePath(uri, queryString);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - reqInfo.getStartTime();
            int status = response.getStatus();

            String statusPhrase;
            try {
                statusPhrase = HttpStatus.valueOf(status).getReasonPhrase();
            } catch (Exception ignored) {
                statusPhrase = "";
            }

            Long contentSize = null;
            if ("POST".equalsIgnoreCase(method) && uri.contains("/upload")) {
                long requestLength = request.getContentLengthLong();
                if (requestLength > 0) contentSize = requestLength;
            } else if ("GET".equalsIgnoreCase(method) && uri.contains("/download")) {
                String responseLength = response.getHeader("Content-Length");
                if (responseLength != null) {
                    try { contentSize = Long.parseLong(responseLength); } catch (NumberFormatException ignored) {}
                }
            }

            String sizeStr = (contentSize != null && contentSize > 0)
                    ? " | " + ConsoleLogger.formatSize(contentSize)
                    : "";

            // Noise reduction for frequent health check probes
            if ("/api/health".equals(uri)) {
                if (status == 200) {
                    log.debug("[HTTP] {} {} | {} {} | {} ms", method, safePath, status, statusPhrase, durationMs);
                } else {
                    log.warn("[HTTP] {} {} | {} {} | {} ms | Health check degraded", method, safePath, status, statusPhrase, durationMs);
                }
            } else {
                String noteStr = "";
                if (status == 404) noteStr = " | Resource not found";
                else if (status == 401) noteStr = " | Unauthorized";
                else if (status == 403) noteStr = " | Access denied";
                else if (status == 409) noteStr = " | Duplicate detected";
                else if (status == 429) noteStr = " | Rate limit active";

                if (status >= 500) {
                    log.error("[HTTP] {} {} | {} {} | {} ms{}{}", method, safePath, status, statusPhrase, durationMs, sizeStr, noteStr);
                } else if (status >= 400) {
                    log.warn("[HTTP] {} {} | {} {} | {} ms{}{}", method, safePath, status, statusPhrase, durationMs, sizeStr, noteStr);
                } else {
                    log.info("[HTTP] {} {} | {} {} | {} ms{}{}", method, safePath, status, statusPhrase, durationMs, sizeStr, noteStr);
                }
            }

            // Always clear MDC and RequestContext state
            RequestContext.clear();
        }
    }

    private String sanitizePath(String uri, String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return uri;
        }
        String[] pairs = queryString.split("&");
        StringBuilder sanitized = new StringBuilder(uri).append("?");
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) sanitized.append("&");
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq).toLowerCase();
                if (key.contains("password") || key.contains("secret") || key.contains("token") || key.contains("key") || key.contains("auth")) {
                    sanitized.append(pair.substring(0, eq)).append("=[REDACTED]");
                } else {
                    sanitized.append(pair);
                }
            } else {
                sanitized.append(pair);
            }
        }
        return sanitized.toString();
    }
}