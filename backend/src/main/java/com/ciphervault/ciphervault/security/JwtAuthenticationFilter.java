package com.ciphervault.ciphervault.security;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
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

        System.out.println(
                "[SUCCESS] JwtAuthenticationFilter initialized."
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        System.out.println(
                "[INFO] JWT filter processing: "
                        + request.getMethod()
                        + " "
                        + requestPath
        );

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null) {

            System.out.println(
                    "[INFO] No Authorization header."
            );

            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            System.out.println(
                    "[WARN] Authorization header is not a Bearer token."
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (token.isBlank()) {

            System.out.println(
                    "[ERROR] Bearer token is empty."
            );

            filterChain.doFilter(request, response);
            return;
        }

        try {

            System.out.println(
                    "[INFO] Extracting email from JWT..."
            );

            String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {

                System.out.println(
                        "[ERROR] JWT email is missing."
                );

                filterChain.doFilter(request, response);
                return;
            }

            System.out.println(
                    "[SUCCESS] JWT email extracted: "
                            + email
            );

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() != null) {

                System.out.println(
                        "[INFO] Authentication already exists."
                );

                filterChain.doFilter(request, response);
                return;
            }

            System.out.println(
                    "[INFO] Looking up user in database..."
            );

            User user = userRepository
                    .findByEmail(email)
                    .orElse(null);

            if (user == null) {

                System.out.println(
                        "[ERROR] User does not exist: "
                                + email
                );

                filterChain.doFilter(request, response);
                return;
            }

            System.out.println(
                    "[SUCCESS] User found: "
                            + email
            );

            System.out.println(
                    "[INFO] Validating JWT..."
            );

            if (!jwtService.isTokenValid(token, email)) {

                System.out.println(
                        "[ERROR] JWT validation failed."
                );

                filterChain.doFilter(request, response);
                return;
            }

            System.out.println(
                    "[SUCCESS] JWT validation successful."
            );

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

            System.out.println(
                    "[SUCCESS] Authentication stored in SecurityContext."
            );

            System.out.println(
                    "[SUCCESS] Authenticated user: "
                            + SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .getName()
            );

            System.out.println(
                    "[SUCCESS] Authentication status: "
                            + SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .isAuthenticated()
            );

        } catch (Exception e) {

            System.out.println(
                    "[ERROR] JWT authentication failed."
            );

            System.out.println(
                    "[ERROR] "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
            );

            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}