package com.project.performanceTrack.dto;

import com.project.performanceTrack.enums.GoalCategory;
import com.project.performanceTrack.enums.GoalPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
// Create goal request DTO
@Data
public class CreateGoalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String desc;

    @NotNull(message = "Category is required")
    private GoalCategory cat;

    @NotNull(message = "Priority is required")
    private GoalPriority pri;

    @NotNull(message = "Start date is required")
    private LocalDate startDt;

    @NotNull(message = "End date is required")
    private LocalDate endDt;

    @NotNull(message = "Manager ID is required")
    private Integer mgrId;
}