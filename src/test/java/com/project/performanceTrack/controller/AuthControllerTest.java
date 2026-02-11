package com.project.performanceTrack.controller;

/*
 * This file tests the AuthController class.
 *
 * Think of it like a quality check for your login/logout/password endpoints.
 * We don't start the whole Spring application - we just test the controller
 * as a plain Java class to keep things fast and simple.
 *
 * The key idea: we "fake" the service layer so we can focus only on
 * testing whether the controller does ITS job correctly.
 */

import com.project.performanceTrack.dto.LoginRequest;
import com.project.performanceTrack.dto.LoginResponse;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.exception.UnauthorizedException;
import com.project.performanceTrack.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.project.performanceTrack.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/*
 * @ExtendWith(MockitoExtension.class) tells JUnit:
 * "Hey, use Mockito to handle all the @Mock and @InjectMocks annotations below"
 * Mockito is the library that lets us create fake (mock) objects.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    /*
     * @Mock creates a FAKE version of AuthService.
     * It won't actually call the real service or touch the database.
     * We control what it returns in each test.
     * Think of it like a stunt double - looks the same, but does what we tell it to.
     */
    @Mock
    private AuthService authSvc;

    /*
     * @InjectMocks creates the REAL AuthController,
     * but automatically injects our fake authSvc into it.
     * So the controller thinks it's working with a real service, but it's our fake one.
     */
    @InjectMocks
    private AuthController authController;

    /*
     * These are test data objects we'll reuse across multiple tests.
     * Declaring them here so every test can access them.
     */
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private MockHttpServletRequest mockRequest;

    /*
     * @BeforeEach means: "Run this method before EVERY single test"
     * This is where we set up our test data so we don't repeat it in every test.
     * It's like a "reset" button that runs before each test.
     */
    @BeforeEach
    void setUp() {

        /*
         * Create a sample login request with test credentials.
         * This is what a user would send when logging in.
         */
        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        /*
         * Create a sample login response with a fake JWT token.
         * This is what we expect the controller to return after a successful login.
         */
        loginResponse = new LoginResponse(
                "fake-jwt-token", 1, "John Doe",
                "john@example.com", UserRole.EMPLOYEE, "Engineering"
        );

        /*
         * MockHttpServletRequest simulates an HTTP request.
         * In the real app, the JwtAuthFilter reads the JWT token and sets
         * "userId" as a request attribute. Here we set it manually since
         * we're not running the full filter chain.
         */
        mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("userId", 1);
    }

    /*
     * ==================== TESTS FOR login() ====================
     */

    /*
     * TEST 1: Happy path - what should happen when login works correctly.
     *
     * @Test marks this as a test method that JUnit should run.
     * @DisplayName gives it a readable description that shows in test reports.
     */
    @Test
    @DisplayName("login() should return success ApiResponse with login data")
    void login_WithValidCredentials_ShouldReturnSuccessResponse() {

        /*
         * ARRANGE: Set up the fake behavior.
         * "When authSvc.login() is called with any LoginRequest object,
         * return our prepared loginResponse instead of calling the real service"
         *
         * any(LoginRequest.class) means "I don't care what the input is, just match any LoginRequest"
         */
        when(authSvc.login(any(LoginRequest.class))).thenReturn(loginResponse);

        /*
         * ACT: Actually call the method we're testing.
         * This is the one line that runs the real controller code.
         */
        ApiResponse<LoginResponse> response = authController.login(loginRequest);

        /*
         * ASSERT: Check that the response is what we expected.
         * assertEquals(expected, actual) - if they don't match, the test FAILS.
         * assertNotNull - makes sure the value isn't null.
         */
        assertEquals("success", response.getStatus());
        assertEquals("Login successful", response.getMsg());
        assertNotNull(response.getData());
        assertEquals("fake-jwt-token", response.getData().getToken());
        assertEquals(1, response.getData().getUserId());
    }

    /*
     * TEST 2: Sad path - what happens when login fails (wrong credentials).
     *
     * We test that the controller doesn't swallow the exception -
     * it should let it bubble up so the GlobalExceptionHandler can catch it.
     */
    @Test
    @DisplayName("login() should propagate UnauthorizedException from service")
    void login_WithInvalidCredentials_ShouldPropagateException() {

        /*
         * ARRANGE: Make the fake service throw an exception instead of returning a response.
         * This simulates what happens when the user enters wrong credentials.
         */
        when(authSvc.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        /*
         * ASSERT: Check that calling login() with bad credentials DOES throw the exception.
         * assertThrows(ExceptionClass, code) - passes if the code throws that exception,
         * fails if it doesn't throw anything or throws a different exception.
         */
        assertThrows(UnauthorizedException.class, () -> authController.login(loginRequest));
    }

    /*
     * ==================== TESTS FOR logout() ====================
     */

    @Test
    @DisplayName("logout() should return success ApiResponse")
    void logout_ShouldReturnSuccessResponse() {

        /*
         * ACT: Call logout with our fake HTTP request (which has userId=1 set).
         * No arrangement needed here because we don't care what authSvc.logout() returns.
         */
        ApiResponse<Void> response = authController.logout(mockRequest);

        /*
         * ASSERT: Check the response AND verify the service was called with the right userId.
         * verify(authSvc).logout(1) means: "Make sure logout() was called exactly once with userId=1"
         * If it was never called, or called with a different ID, the test fails.
         */
        assertEquals("success", response.getStatus());
        assertEquals("Logout successful", response.getMsg());
        verify(authSvc).logout(1);
    }

    /*
     * ==================== TESTS FOR changePassword() ====================
     */

    @Test
    @DisplayName("changePassword() should return success ApiResponse")
    void changePassword_ShouldReturnSuccessResponse() {

        /*
         * ARRANGE: Build the request body map with old and new passwords.
         * This simulates what the user sends in the request body.
         */
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "oldPass");
        body.put("newPassword", "newPass");

        /*
         * ACT: Call changePassword with our body map and fake HTTP request.
         */
        ApiResponse<Void> response = authController.changePassword(body, mockRequest);

        /*
         * ASSERT: Check success response AND verify the service was called
         * with the correct userId, old password, and new password.
         */
        assertEquals("success", response.getStatus());
        assertEquals("Password changed successfully", response.getMsg());
        verify(authSvc).changePassword(1, "oldPass", "newPass");
    }
}