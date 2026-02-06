package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.CreateUserRequest;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// User management controller
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {


    private final UserService userSvc;

    // Get all users (Admin/Manager)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<User>> getAllUsers() {
        List<User> users = userSvc.getAllUsers();
        return ApiResponse.success("Users retrieved", users);
    }

    // Get user by ID
    @GetMapping("/{userId}")
    public ApiResponse<User> getUserById(@PathVariable Integer userId) {
        User user = userSvc.getUserById(userId);
        return ApiResponse.success("User retrieved", user);
    }

    // Create new user (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest req,
                                        HttpServletRequest httpReq) {
        Integer adminId = (Integer) httpReq.getAttribute("userId");
        User user = userSvc.createUser(req, adminId);
        return ApiResponse.success("User created", user);
    }

    // Update user (Admin only)
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> updateUser(@PathVariable Integer userId,
                                        @Valid @RequestBody CreateUserRequest req,
                                        HttpServletRequest httpReq) {
        Integer adminId = (Integer) httpReq.getAttribute("userId");
        User user = userSvc.updateUser(userId, req, adminId);
        return ApiResponse.success("User updated", user);
    }

    // Get team members (Manager)
    @GetMapping("/{userId}/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<List<User>> getTeam(@PathVariable Integer userId) {
        List<User> team = userSvc.getTeamMembers(userId);
        return ApiResponse.success("Team members retrieved", team);
    }
}
