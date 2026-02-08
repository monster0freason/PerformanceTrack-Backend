package com.project.performanceTrack.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil.
 *
 * WHAT IS JWT?
 * ------------
 * JWT (JSON Web Token) is a compact token format used for authentication.
 * After a user logs in, the server generates a JWT containing user info.
 * The client sends this token with every request to prove they're authenticated.
 *
 * JwtUtil handles:
 * 1. Generating tokens (with email, userId, and role)
 * 2. Extracting info from tokens
 * 3. Validating tokens (checking expiration and integrity)
 *
 * NOTE: JwtUtil is a plain @Component, not dependent on other Spring beans,
 * so we can test it without Mockito - just create it directly!
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String validToken;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Generate a token to use in our tests
        validToken = jwtUtil.generateToken("john@example.com", 1, "EMPLOYEE");
    }

    // ==================== generateToken() ====================

    @Test
    @DisplayName("generateToken() should create a non-null, non-empty token")
    void generateToken_ShouldCreateToken() {
        String token = jwtUtil.generateToken("test@example.com", 42, "ADMIN");

        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");

        // JWT tokens have 3 parts separated by dots: header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts separated by dots");
    }

    // ==================== extractEmail() ====================

    @Test
    @DisplayName("extractEmail() should return the email used to generate the token")
    void extractEmail_ShouldReturnCorrectEmail() {
        String email = jwtUtil.extractEmail(validToken);

        assertEquals("john@example.com", email);
    }

    // ==================== extractUserId() ====================

    @Test
    @DisplayName("extractUserId() should return the userId used to generate the token")
    void extractUserId_ShouldReturnCorrectUserId() {
        Integer userId = jwtUtil.extractUserId(validToken);

        assertEquals(1, userId);
    }

    // ==================== extractRole() ====================

    @Test
    @DisplayName("extractRole() should return the role used to generate the token")
    void extractRole_ShouldReturnCorrectRole() {
        String role = jwtUtil.extractRole(validToken);

        assertEquals("EMPLOYEE", role);
    }

    // ==================== validateToken() ====================

    @Test
    @DisplayName("validateToken() should return true for a valid token with matching email")
    void validateToken_WithValidTokenAndEmail_ShouldReturnTrue() {
        Boolean isValid = jwtUtil.validateToken(validToken, "john@example.com");

        assertTrue(isValid, "Token should be valid for the correct email");
    }

    @Test
    @DisplayName("validateToken() should return false for wrong email")
    void validateToken_WithWrongEmail_ShouldReturnFalse() {
        Boolean isValid = jwtUtil.validateToken(validToken, "wrong@example.com");

        assertFalse(isValid, "Token should be invalid for a different email");
    }

    // ==================== Different roles ====================

    @Test
    @DisplayName("Token should store different roles correctly")
    void generateToken_WithDifferentRoles_ShouldStoreRoleCorrectly() {
        String adminToken = jwtUtil.generateToken("admin@example.com", 100, "ADMIN");
        String managerToken = jwtUtil.generateToken("mgr@example.com", 50, "MANAGER");

        assertEquals("ADMIN", jwtUtil.extractRole(adminToken));
        assertEquals("MANAGER", jwtUtil.extractRole(managerToken));
    }

    @Test
    @DisplayName("Each generated token should be unique")
    void generateToken_ShouldCreateUniqueTokens() {
        String token1 = jwtUtil.generateToken("john@example.com", 1, "EMPLOYEE");
        String token2 = jwtUtil.generateToken("john@example.com", 1, "EMPLOYEE");

        // Tokens will differ because they include the current timestamp
        assertNotEquals(token1, token2, "Tokens generated at different times should be different");
    }
}
