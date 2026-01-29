package com.project.performanceTrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Login request DTO
@Data // A Lombok annotation that generates Getters, Setters, toString, equals, and hashCode at compile-time
public class LoginRequest {

    // Ensures the field is not null and the trimmed length is greater than zero
    @NotBlank(message = "Email is required")
    // Validates that the string follows a standard email format (e.g., user@domain.com)
    @Email(message = "Email must be valid")
    private String email;

    // Ensures the password is not null, not empty, and not just whitespace
    @NotBlank(message = "Password is required")
    private String password;
}
