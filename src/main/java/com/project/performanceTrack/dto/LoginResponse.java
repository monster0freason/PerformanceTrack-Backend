package com.project.performanceTrack.dto;

import com.project.performanceTrack.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Login response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;       // JWT token
    private Integer userId;
    private String name;
    private String email;
    private UserRole role;
    private String department;
}
