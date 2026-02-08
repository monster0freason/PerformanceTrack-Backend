package com.project.performanceTrack.controller;

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

/**
 * Unit tests for AuthController.
 *
 * CONTROLLER TESTING APPROACH:
 * ----------------------------
 * We test controllers as plain Java objects (not with MockMvc) to keep things simple.
 * We mock the service layer and verify that:
 * 1. The controller calls the correct service method
 * 2. The controller wraps the result in an ApiResponse correctly
 * 3. Error cases are handled properly
 *
 * NOTE: MockHttpServletRequest is used to simulate HTTP request attributes
 * that would normally be set by the JwtAuthFilter.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authSvc;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        loginResponse = new LoginResponse(
                "fake-jwt-token", 1, "John Doe",
                "john@example.com", UserRole.EMPLOYEE, "Engineering"
        );

        // MockHttpServletRequest simulates the HTTP request
        // In real app, JwtAuthFilter sets "userId" attribute from the JWT token
        mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("userId", 1);
    }

    // ==================== login() ====================

    @Test
    @DisplayName("login() should return success ApiResponse with login data")
    void login_WithValidCredentials_ShouldReturnSuccessResponse() {
        when(authSvc.login(any(LoginRequest.class))).thenReturn(loginResponse);

        ApiResponse<LoginResponse> response = authController.login(loginRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Login successful", response.getMsg());
        assertNotNull(response.getData());
        assertEquals("fake-jwt-token", response.getData().getToken());
        assertEquals(1, response.getData().getUserId());
    }

    @Test
    @DisplayName("login() should propagate UnauthorizedException from service")
    void login_WithInvalidCredentials_ShouldPropagateException() {
        when(authSvc.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        assertThrows(UnauthorizedException.class, () -> authController.login(loginRequest));
    }

    // ==================== logout() ====================

    @Test
    @DisplayName("logout() should return success ApiResponse")
    void logout_ShouldReturnSuccessResponse() {
        ApiResponse<Void> response = authController.logout(mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Logout successful", response.getMsg());
        verify(authSvc).logout(1);
    }

    // ==================== changePassword() ====================

    @Test
    @DisplayName("changePassword() should return success ApiResponse")
    void changePassword_ShouldReturnSuccessResponse() {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "oldPass");
        body.put("newPassword", "newPass");

        ApiResponse<Void> response = authController.changePassword(body, mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Password changed successfully", response.getMsg());
        verify(authSvc).changePassword(1, "oldPass", "newPass");
    }
}
