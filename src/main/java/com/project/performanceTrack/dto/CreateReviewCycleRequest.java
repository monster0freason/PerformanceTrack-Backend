package com.project.performanceTrack.dto;

import com.project.performanceTrack.enums.ReviewCycleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

// Create review cycle request DTO
@Data
public class CreateReviewCycleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Start date is required")
    private LocalDate startDt;

    @NotNull(message = "End date is required")
    private LocalDate endDt;

    @NotNull(message = "Status is required")
    private ReviewCycleStatus status;

    private Boolean reqCompAppr = true;  // Requires completion approval

    private Boolean evReq = true;        // Evidence required
}
