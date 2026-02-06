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

// Authentication controller - handles login/logout
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // Login endpoint
    private final AuthService authSvc;


    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse resp = authSvc.login(req);

        return ApiResponse.success("Login successful", resp);
    }

    // Logout endpoint
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest req) {

        Integer userId = (Integer) req.getAttribute("userId");


        authSvc.logout(userId);

        return ApiResponse.success("Logout successful");
    }

    // Change password endpoint
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
