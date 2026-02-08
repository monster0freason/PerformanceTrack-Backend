package com.project.performanceTrack.exception;

import com.project.performanceTrack.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * WHAT THIS TESTS:
 * ----------------
 * The GlobalExceptionHandler catches exceptions thrown by controllers
 * and converts them into user-friendly API responses with proper HTTP status codes.
 *
 * This is tested without Spring context - just plain Java testing.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // ==================== ResourceNotFoundException ====================

    @Test
    @DisplayName("handleNotFound() should return 404 with error message")
    void handleNotFound_ShouldReturn404() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        // Act
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleNotFound(ex);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "HTTP status should be 404");
        assertEquals("error", response.getBody().getStatus());
        assertEquals("User not found", response.getBody().getMsg());
        assertNull(response.getBody().getData(), "No data should be returned for errors");
    }

    // ==================== UnauthorizedException ====================

    @Test
    @DisplayName("handleUnauthorized() should return 401 with error message")
    void handleUnauthorized_ShouldReturn401() {
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleUnauthorized(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "HTTP status should be 401");
        assertEquals("error", response.getBody().getStatus());
        assertEquals("Invalid credentials", response.getBody().getMsg());
    }

    // ==================== BadRequestException ====================

    @Test
    @DisplayName("handleBadRequest() should return 400 with error message")
    void handleBadRequest_ShouldReturn400() {
        BadRequestException ex = new BadRequestException("Email already exists");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400");
        assertEquals("error", response.getBody().getStatus());
        assertEquals("Email already exists", response.getBody().getMsg());
    }

    // ==================== Validation Errors ====================

    @Test
    @DisplayName("handleValidation() should return 400 with field-level error details")
    void handleValidation_ShouldReturn400WithFieldErrors() {
        // Arrange: Create a MethodArgumentNotValidException with field errors
        // BeanPropertyBindingResult is a Spring class that collects validation errors
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        // Act
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleValidation(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("error", response.getBody().getStatus());
        assertTrue(response.getBody().getMsg().contains("Validation failed"));
        assertTrue(response.getBody().getMsg().contains("email"));
        assertTrue(response.getBody().getMsg().contains("password"));
    }

    // ==================== General Exception ====================

    @Test
    @DisplayName("handleGeneral() should return 500 for unexpected errors")
    void handleGeneral_ShouldReturn500() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "HTTP status should be 500");
        assertEquals("error", response.getBody().getStatus());
        assertTrue(response.getBody().getMsg().contains("Something went wrong"));
    }

    @Test
    @DisplayName("handleGeneral() should handle null message gracefully")
    void handleGeneral_WithNullMessage_ShouldStillReturn500() {
        Exception ex = new NullPointerException();

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("error", response.getBody().getStatus());
    }
}
