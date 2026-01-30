package com.project.performanceTrack.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelfAssessmentRequest {

    @NotNull(message = "CycleId is required")
    private Integer cycleId;

    @NotNull(message = "Self-assessment data is required")
    private String selfAssmt;

    @NotNull(message = "Self-rating is required")
    private Integer selfRating;
}