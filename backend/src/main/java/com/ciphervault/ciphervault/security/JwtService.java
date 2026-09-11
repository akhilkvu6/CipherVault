package com.ciphervault.ciphervault.security;

import com.ciphervault.ciphervault.util.ConsoleLogger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "CipherVaultSecretKeyForJWTAuthentication2026SecureKey123456";

    private static final long EXPIRATION_TIME =
            24 * 60 * 60 * 1000L; // 24 hours

    private final SecretKey secretKey;

    public JwtService() {

        ConsoleLogger.info(
                "Initializing JWT service..."
        );

        this.secretKey = Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8)
        );

        ConsoleLogger.success(
                "JWT service initialized successfully."
        );
    }

    public String generateToken(String email) {

        ConsoleLogger.info(
                "Generating JWT token for: " + email
        );

        String token = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION_TIME
                        )
                )
                .signWith(secretKey)
                .compact();

        ConsoleLogger.success(
                "JWT token generated successfully for: " + email
        );

        return token;
    }

    public String extractEmail(String token) {

        ConsoleLogger.info(
                "Extracting email from JWT token."
        );

        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {

        ConsoleLogger.info(
                "Extracting expiration time from JWT token."
        );

        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

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

    public boolean isTokenValid(
            String token,
            String email) {

        try {

            String extractedEmail = extractEmail(token);

            boolean valid = extractedEmail.equals(email)
                    && !isTokenExpired(token);

            if (valid) {

                ConsoleLogger.success(
                        "JWT validation successful for: "
                                + email
                );

            } else {

                ConsoleLogger.warn(
                        "JWT validation failed for: "
                                + email
                );
            }

            return valid;

        } catch (Exception e) {

            ConsoleLogger.warn(
                    "JWT validation failed: invalid or malformed token."
            );

            return false;
        }
    }

    private boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }
}