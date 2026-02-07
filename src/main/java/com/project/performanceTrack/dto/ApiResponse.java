package com.project.performanceTrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

// Standard API response wrapper
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;  // "success" or "error"
    private String msg;     // Message
    private T data;         // Response data

    // Success response
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>("success", msg, data);
    }

    // Success response without data
    public static <T> ApiResponse<T> success(String msg) {
        return new ApiResponse<>("success", msg, null);
    }

    // Error response
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>("error", msg, null);
    }


    public static <T> ApiResponse<PageResponse<T>> successPage(String msg, Page<T> page) {
        return new ApiResponse<>("success", msg, new PageResponse<>(page));
    }
}
