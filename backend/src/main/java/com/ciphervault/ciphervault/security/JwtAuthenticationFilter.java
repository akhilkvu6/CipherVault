package com.ciphervault.ciphervault.security;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.util.ConsoleLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    private final SecurityContextRepository securityContextRepository =
            new RequestAttributeSecurityContextRepository();

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;

        ConsoleLogger.success(
                "JwtAuthenticationFilter initialized successfully."
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        ConsoleLogger.info(
                "JWT authentication request: "
                        + request.getMethod()
                        + " "
                        + requestPath
        );

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null) {

            ConsoleLogger.info(
                    "No Authorization header found. "
                            + "Continuing without JWT authentication."
            );

            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            ConsoleLogger.warn(
                    "Authorization header is not a Bearer token."
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (token.isBlank()) {

            ConsoleLogger.warn(
                    "Bearer token is empty."
            );

            filterChain.doFilter(request, response);
            return;
        }

        try {

            String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {

                ConsoleLogger.warn(
                        "JWT authentication failed: email is missing."
                );

                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() != null) {

                ConsoleLogger.info(
                        "Authentication already exists for this request."
                );

                filterChain.doFilter(request, response);
                return;
            }

            ConsoleLogger.info(
                    "Looking up authenticated user in database: "
                            + email
            );

            User user = userRepository
                    .findByEmail(email)
                    .orElse(null);

            if (user == null) {

                ConsoleLogger.warn(
                        "JWT authentication failed: user not found: "
                                + email
                );

                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isTokenValid(token, email)) {

                ConsoleLogger.warn(
                        "JWT authentication failed: invalid or expired token."
                );

                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_USER"
                                    )
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            /*
             * Explicitly save the SecurityContext for this request.
             */
            securityContextRepository.saveContext(
                    context,
                    request,
                    response
            );

            ConsoleLogger.success(
                    "JWT authentication successful: "
                            + email
            );

        } catch (Exception e) {

            ConsoleLogger.warn(
                    "JWT authentication failed: invalid or malformed token."
            );

            ConsoleLogger.error(
                    "JWT processing error: "
                            + e.getClass().getSimpleName()
            );
        }

        filterChain.doFilter(request, response);
    }
}