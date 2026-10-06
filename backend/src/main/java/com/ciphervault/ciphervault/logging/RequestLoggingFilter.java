package com.ciphervault.ciphervault.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that establishes the RequestContext correlation ID and logs
 * HTTP request lifecycle in a clean, pure-ASCII developer console format.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestLoggingFilter extends OncePerRequestFilter {

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

        String fullPath = (queryString != null && !queryString.isBlank())
                ? uri + "?" + queryString
                : uri;

        RequestContext.RequestInfo reqInfo = RequestContext.init();
        String reqId = reqInfo.getRequestId();

        boolean isDetailedFlow =
                uri.contains("/upload")
                        || uri.contains("/download")
                        || uri.contains("/login")
                        || uri.contains("/register")
                        || uri.contains("/search")
                        || "DELETE".equalsIgnoreCase(method);

        if (isDetailedFlow) {
            ConsoleLogger.logHttpRequestStart(
                    reqId,
                    method,
                    fullPath,
                    null
            );
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs =
                    System.currentTimeMillis() - reqInfo.getStartTime();

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

                if (requestLength > 0) {
                    contentSize = requestLength;
                }
            } else if ("GET".equalsIgnoreCase(method) && uri.contains("/download")) {
                String responseLength = response.getHeader("Content-Length");

                if (responseLength != null) {
                    try {
                        contentSize = Long.parseLong(responseLength);
                    } catch (NumberFormatException ignored) {
                        // Ignore invalid Content-Length values.
                    }
                }
            }

            String note = null;

            if (status == 404) {
                note = "Endpoint or resource not found";
            } else if (status == 401) {
                note = "Authentication required or invalid credentials";
            } else if (status == 409) {
                note = "Duplicate detected";
            } else if (status == 429) {
                note = "Rate limit active";
            }

            if (isDetailedFlow) {
                ConsoleLogger.logHttpResponse(
                        reqId,
                        method,
                        fullPath,
                        status,
                        statusPhrase,
                        contentSize,
                        durationMs,
                        note
                );
            } else {
                String tag = (status >= 200 && status < 400)
                        ? ConsoleLogger.TAG_OK
                        : ConsoleLogger.TAG_ERROR;

                String sizeStr =
                        contentSize != null && contentSize > 0
                                ? " | " + ConsoleLogger.formatSize(contentSize)
                                : "";

                System.out.println("[" + ConsoleLogger.BOLD + reqId + ConsoleLogger.RESET + "] " + ConsoleLogger.TAG_HTTP + " " + method + " " + fullPath + " | " + tag + " " + status + sizeStr + " | " + durationMs + " ms");
            }

            RequestContext.clear();
        }
    }
}