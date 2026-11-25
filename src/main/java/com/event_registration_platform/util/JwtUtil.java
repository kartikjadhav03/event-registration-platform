
package com.event_registration_platform.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private static final long REFRESH_TOKEN_EXPIRATION =
            604800000L; // 7 days

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate Access Token
    public String generateToken(String email, String role) {

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis() + expiration
                ))
                .signWith(getSigningKey())
                .compact();
    }

    // Generate Refresh Token
    public String generateRefreshToken(String email) {

        return Jwts.builder()
                .subject(email)
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis()
                                + REFRESH_TOKEN_EXPIRATION
                ))
                .signWith(getSigningKey())
                .compact();
    }

    // Extract all claims
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract Email
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract Role
    public String extractRole(String token) {
        return extractAllClaims(token)
                .get("role", String.class);
    }

    // Check Access Token
    public boolean isAccessTokenValid(String token) {

        try {
            Claims claims = extractAllClaims(token);

            return "ACCESS".equals(claims.get("type", String.class));

        } catch (Exception e) {
            return false;
        }
    }

    // Check Refresh Token
    public boolean isRefreshTokenValid(String token) {

        try {
            Claims claims = extractAllClaims(token);

            return "REFRESH".equals(claims.get("type", String.class));

        } catch (Exception e) {
            return false;
        }
    }

    // General Token Validation
    public boolean isTokenValid(String token) {

        return isAccessTokenValid(token)
                || isRefreshTokenValid(token);
    }
}
