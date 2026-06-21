package com.arenagamer.api.security;

import com.arenagamer.api.entity.enums.AuthUserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userType", user.getType().name());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("type", "access");
        if (user.getClientUserId() != null) {
            claims.put("clientUserId", user.getClientUserId());
        }
        return buildToken(claims, accessTokenExpiration);
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userType", user.getType().name());
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());
        return buildToken(claims, refreshTokenExpiration);
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private String buildToken(Map<String, Object> claims, Long expirationSeconds) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    public AuthUserType extractUserType(String token) {
        return AuthUserType.valueOf(extractClaims(token).get("userType", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
