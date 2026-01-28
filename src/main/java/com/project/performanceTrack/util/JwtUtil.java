package com.project.performanceTrack.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// JWT utility for token generation and validation
@Component
public class JwtUtil {
    
    // Secret key for JWT signing (in production, use environment variable)
    private static final String SECRET = "MySecretKeyForPerformanceTrackApp2026VeryLongSecretKey";
    private static final long EXPIRATION = 86400000; // 24 hours in milliseconds
    
    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
    
    // Generate JWT token
    public String generateToken(String email, Integer userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // Extract email from token
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
    
    // Extract user ID from token
    public Integer extractUserId(String token) {
        return (Integer) extractClaims(token).get("userId");
    }
    
    // Extract role from token
    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }
    
    // Extract all claims
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    // Validate token
    public Boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }
    
    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
