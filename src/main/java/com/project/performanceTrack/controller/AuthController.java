package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.LoginRequest;
import com.project.performanceTrack.dto.LoginResponse;
import com.project.performanceTrack.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authSvc;

    // Endpoint: POST /api/v1/auth/login
    // Accepts login credentials and returns a JWT if authentication is successful.
    // This is the primary entry point for users to establish a session.
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse resp = authSvc.login(req);
        return ApiResponse.success("Login successful", resp);
    }

    // Endpoint: POST /api/v1/auth/logout
    // Processes the logout request for the currently authenticated user.
    // Triggers the backend audit logging for the session termination.
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest req) {
        Integer userId = (Integer) req.getAttribute("userId");
        authSvc.logout(userId);
        return ApiResponse.success("Logout successful");
    }

    // Endpoint: PUT /api/v1/auth/change-password
    // Allows an authenticated user to update their own password securely.
    // Extracts the user ID from the request attribute for service-layer processing.
    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body,
                                            HttpServletRequest req) {
        Integer userId = (Integer) req.getAttribute("userId");
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        authSvc.changePassword(userId, oldPwd, newPwd);
        return ApiResponse.success("Password changed successfully");
    }
}