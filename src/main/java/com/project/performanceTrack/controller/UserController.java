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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userSvc;

    // Endpoint: GET /api/v1/users
    // Access: Restricted to users with 'ADMIN' or 'MANAGER' roles.
    // Returns a successful API response containing the list of all system users.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<User>> getAllUsers() {
        List<User> users = userSvc.getAllUsers();
        return ApiResponse.success("Users retrieved", users);
    }

    // Endpoint: GET /api/v1/users/{userId}
    // Access: Publicly available within the authenticated context.
    // Returns the detailed profile of a single user by their ID.
    @GetMapping("/{userId}")
    public ApiResponse<User> getUserById(@PathVariable Integer userId) {
        User user = userSvc.getUserById(userId);
        return ApiResponse.success("User retrieved", user);
    }

    // Endpoint: POST /api/v1/users
    // Access: Restricted to 'ADMIN' role only.
    // Extracts the admin's ID from the request context to log the creation event.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest req,
                                        HttpServletRequest httpReq) {
        Integer adminId = (Integer) httpReq.getAttribute("userId");
        User user = userSvc.createUser(req, adminId);
        return ApiResponse.success("User created", user);
    }

    // Endpoint: PUT /api/v1/users/{userId}
    // Access: Restricted to 'ADMIN' role only.
    // Modifies existing user data and records the administrator responsible for the change.
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> updateUser(@PathVariable Integer userId,
                                        @Valid @RequestBody CreateUserRequest req,
                                        HttpServletRequest httpReq) {
        Integer adminId = (Integer) httpReq.getAttribute("userId");
        User user = userSvc.updateUser(userId, req, adminId);
        return ApiResponse.success("User updated", user);
    }

    // Endpoint: GET /api/v1/users/{userId}/team
    // Access: Restricted to 'MANAGER' role only.
    // Provides a manager with a list of all employees reporting directly to them.
    @GetMapping("/{userId}/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<List<User>> getTeam(@PathVariable Integer userId) {
        List<User> team = userSvc.getTeamMembers(userId);
        return ApiResponse.success("Team members retrieved", team);
    }
}