package com.ciphervault.ciphervault.security;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtService jwtService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMissingAuthorizationHeader() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testInvalidBearerToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(jwtService.extractEmail("invalid_token")).thenReturn("test@ciphervault.local");
        when(jwtService.isTokenValid("invalid_token", "test@ciphervault.local")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testExpiredJwt() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer expired_token");
        when(jwtService.extractEmail("expired_token")).thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Expired"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testValidTokenAuthenticatesUser() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtService.extractEmail("valid_token")).thenReturn("test@ciphervault.local");
        when(jwtService.isTokenValid("valid_token", "test@ciphervault.local")).thenReturn(true);
        when(jwtService.extractTokenVersion("valid_token")).thenReturn(1);

        User mockUser = new User();
        mockUser.setEmail("test@ciphervault.local");
        mockUser.setTokenVersion(1);
        when(userRepository.findByEmail("test@ciphervault.local")).thenReturn(Optional.of(mockUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testMissingTokenVersionRejectsToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer missing_version_token");
        when(jwtService.extractEmail("missing_version_token")).thenReturn("test@ciphervault.local");
        when(jwtService.isTokenValid("missing_version_token", "test@ciphervault.local")).thenReturn(true);
        when(jwtService.extractTokenVersion("missing_version_token")).thenReturn(null);

        User mockUser = new User();
        mockUser.setEmail("test@ciphervault.local");
        mockUser.setTokenVersion(1);
        when(userRepository.findByEmail("test@ciphervault.local")).thenReturn(Optional.of(mockUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testTamperedTokenException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer tampered_token");
        when(jwtService.extractEmail("tampered_token")).thenThrow(new io.jsonwebtoken.MalformedJwtException("Tampered"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testOutdatedTokenVersionRejectsToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer outdated_token");
        when(jwtService.extractEmail("outdated_token")).thenReturn("test@ciphervault.local");
        when(jwtService.isTokenValid("outdated_token", "test@ciphervault.local")).thenReturn(true);
        when(jwtService.extractTokenVersion("outdated_token")).thenReturn(1);

        User mockUser = new User();
        mockUser.setEmail("test@ciphervault.local");
        mockUser.setTokenVersion(2);
        when(userRepository.findByEmail("test@ciphervault.local")).thenReturn(Optional.of(mockUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
