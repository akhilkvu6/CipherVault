package com.ciphervault.ciphervault.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String DEFAULT_SECRET_KEY =
            "CipherVaultSecretKeyForJWTAuthentication2026SecureKey123456";

    private static final long DEFAULT_EXPIRATION_TIME =
            24 * 60 * 60 * 1000L; // 24 hours

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtService() {
        this(DEFAULT_SECRET_KEY, DEFAULT_EXPIRATION_TIME, "development", true);
    }

    @Autowired
    public JwtService(
            @Value("${ciphervault.jwt.secret:}") String secretKeyString,
            @Value("${ciphervault.jwt.expiration-ms:86400000}") long expirationTime,
            @Value("${ciphervault.environment:development}") String environment,
            @Value("${ciphervault.security.dev-defaults-enabled:true}") boolean devDefaultsEnabled) {
        log.debug("Initializing JWT service (environment: {})...", environment);
        this.expirationTime = expirationTime;

        boolean isProd = "production".equalsIgnoreCase(environment) || !devDefaultsEnabled;
        if (secretKeyString == null || secretKeyString.trim().isEmpty() || (isProd && DEFAULT_SECRET_KEY.equals(secretKeyString))) {
            if (isProd) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: Required JWT secret is missing or insecure in production environment. " +
                        "Configure CIPHERVAULT_JWT_SECRET environment variable.");
            }
            log.warn("SECURITY WARNING: CIPHERVAULT_JWT_SECRET is unset. Running with development JWT secret fallback.");
            secretKeyString = DEFAULT_SECRET_KEY;
        }

        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        log.debug("JWT service initialized successfully.");
    }

    public String generateToken(String email, int tokenVersion) {
        log.debug("Generating JWT token for: {} (version: {})", email, tokenVersion);
        return Jwts.builder()
                .subject(email)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    public String generateToken(String email) {
        return generateToken(email, 1);
    }

    public Integer extractTokenVersion(String token) {
        try {
            return extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
        } catch (Exception e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(email) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}