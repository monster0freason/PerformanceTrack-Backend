package com.project.performanceTrack.dto;

import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Create user request DTO
@Data
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    private String dept;

    private Integer mgrId;  // Manager ID

    private UserStatus status = UserStatus.ACTIVE;
}
