package com.project.performanceTrack.exception;

/*
 * This file tests the GlobalExceptionHandler class.
 *
 * The GlobalExceptionHandler is like a safety net - whenever something goes wrong
 * anywhere in the application, it catches the exception and converts it into
 * a clean, user-friendly JSON response with the right HTTP status code.
 *
 * For example:
 * - User not found? → 404 response
 * - Wrong password? → 401 response
 * - Bad input? → 400 response
 * - Server crashed? → 500 response
 *
 * These tests verify that each exception type produces the correct HTTP status
 * and response format.
 */

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

class GlobalExceptionHandlerTest {

    /*
     * We don't use @Mock or @InjectMocks here because GlobalExceptionHandler
     * has no dependencies - it's a simple class we can just create directly.
     */
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {

        /*
         * Create a fresh instance of the handler before each test.
         * Simple and straightforward - no mocking needed!
         */
        exceptionHandler = new GlobalExceptionHandler();
    }

    /*
     * ==================== TEST FOR ResourceNotFoundException (404) ====================
     */

    @Test
    @DisplayName("handleNotFound() should return 404 with error message")
    void handleNotFound_ShouldReturn404() {

        /*
         * ARRANGE: Create the exception that would be thrown when
         * something is not found in the database (like a user or goal that doesn't exist).
         */
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        /*
         * ACT: Pass the exception to the handler, just like Spring would do automatically.
         */
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleNotFound(ex);

        /*
         * ASSERT:
         * - HTTP status should be 404 (Not Found)
         * - Response body status should be "error"
         * - Message should match the exception message
         * - Data should be null (no data when there's an error)
         */
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "HTTP status should be 404");
        assertEquals("error", response.getBody().getStatus());
        assertEquals("User not found", response.getBody().getMsg());
        assertNull(response.getBody().getData(), "No data should be returned for errors");
    }

    /*
     * ==================== TEST FOR UnauthorizedException (401) ====================
     */

    @Test
    @DisplayName("handleUnauthorized() should return 401 with error message")
    void handleUnauthorized_ShouldReturn401() {

        /*
         * This exception is thrown when someone tries to do something
         * they're not allowed to - like logging in with wrong credentials.
         */
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleUnauthorized(ex);

        /*
         * 401 means "Unauthorized" - you need to prove who you are first!
         */
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "HTTP status should be 401");
        assertEquals("error", response.getBody().getStatus());
        assertEquals("Invalid credentials", response.getBody().getMsg());
    }

    /*
     * ==================== TEST FOR BadRequestException (400) ====================
     */

    @Test
    @DisplayName("handleBadRequest() should return 400 with error message")
    void handleBadRequest_ShouldReturn400() {

        /*
         * This exception is thrown when the user sends invalid/bad data.
         * For example: trying to register with an email that already exists.
         */
        BadRequestException ex = new BadRequestException("Email already exists");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBadRequest(ex);

        /*
         * 400 means "Bad Request" - the user sent something wrong.
         */
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400");
        assertEquals("error", response.getBody().getStatus());
        assertEquals("Email already exists", response.getBody().getMsg());
    }

    /*
     * ==================== TEST FOR VALIDATION ERRORS (400) ====================
     */

    @Test
    @DisplayName("handleValidation() should return 400 with field-level error details")
    void handleValidation_ShouldReturn400WithFieldErrors() {

        /*
         * ARRANGE: This is more complex - we need to simulate Spring's validation failing.
         *
         * BeanPropertyBindingResult is a Spring class that collects all validation errors.
         * It's like a bag that holds all the "this field is wrong" errors.
         */
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        /*
         * Add two field errors manually - simulating what happens when
         * a user submits a form without filling in required fields.
         *
         * FieldError(objectName, fieldName, errorMessage)
         */
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        /*
         * Wrap all those errors into a MethodArgumentNotValidException,
         * which is what Spring throws when @Valid validation fails.
         */
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        /*
         * ACT: Handle the validation exception.
         */
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleValidation(ex);

        /*
         * ASSERT:
         * - Status should be 400
         * - Message should mention "Validation failed"
         * - Message should include WHICH fields failed ("email" and "password")
         *
         * assertTrue(x.contains(y)) checks if the string contains the expected text.
         */
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("error", response.getBody().getStatus());
        assertTrue(response.getBody().getMsg().contains("Validation failed"));
        assertTrue(response.getBody().getMsg().contains("email"));
        assertTrue(response.getBody().getMsg().contains("password"));
    }

    /*
     * ==================== TESTS FOR GENERAL/UNEXPECTED EXCEPTIONS (500) ====================
     */

    @Test
    @DisplayName("handleGeneral() should return 500 for unexpected errors")
    void handleGeneral_ShouldReturn500() {

        /*
         * This covers any unexpected exception that we didn't plan for.
         * Like a RuntimeException thrown from somewhere deep in the code.
         *
         * 500 means "Internal Server Error" - something broke on our end.
         */
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "HTTP status should be 500");
        assertEquals("error", response.getBody().getStatus());
        assertTrue(response.getBody().getMsg().contains("Something went wrong"));
    }

    @Test
    @DisplayName("handleGeneral() should handle null message gracefully")
    void handleGeneral_WithNullMessage_ShouldStillReturn500() {

        /*
         * Edge case: What if the exception has NO message?
         * For example, a NullPointerException without any message.
         *
         * The handler should still return a 500 without crashing itself!
         * This tests that our error handler is robust enough to handle
         * even the messiest situations.
         */
        Exception ex = new NullPointerException();

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("error", response.getBody().getStatus());
    }
}