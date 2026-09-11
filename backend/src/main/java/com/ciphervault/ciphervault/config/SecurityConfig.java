package com.ciphervault.ciphervault.config;

import com.ciphervault.ciphervault.security.JwtAuthenticationFilter;
import com.ciphervault.ciphervault.util.ConsoleLogger;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        ConsoleLogger.success(
                "SecurityConfig initialized successfully."
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        ConsoleLogger.info(
                "Configuring Spring Security..."
        );

        http
                /*
                 * CipherVault is a stateless REST API.
                 * CSRF protection is therefore disabled.
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * JWT authentication does not use HTTP sessions.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Configure which endpoints require authentication.
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Login and registration are public.
                         */
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        /*
                         * Spring Boot error endpoint must be accessible
                         * when an internal request is forwarded to /error.
                         */
                        .requestMatchers("/error")
                        .permitAll()

                        /*
                         * Every other endpoint requires JWT authentication.
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * Return HTTP 401 for unauthenticated requests.
                 */
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request, response, authException) -> {

                                    ConsoleLogger.warn(
                                            "Authentication required for: "
                                                    + request.getRequestURI()
                                    );

                                    response.sendError(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized"
                                    );
                                }
                        )
                )

                /*
                 * Disable HTTP Basic authentication.
                 */
                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )

                /*
                 * Disable browser form login.
                 */
                .formLogin(formLogin ->
                        formLogin.disable()
                )

                /*
                 * Process JWT before Spring's normal
                 * username/password authentication filter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        ConsoleLogger.success(
                "Spring Security configured successfully."
        );

        return http.build();
    }
}