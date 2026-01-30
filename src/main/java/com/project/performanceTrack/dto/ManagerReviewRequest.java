package com.project.performanceTrack.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManagerReviewRequest {
    @NotNull(message = "Manager feedback is required")
    private String mgrFb;

    @NotNull(message = "Manager rating is required")
    private Integer mgrRating;

    private String ratingJust;

    private String compRec;

    private String nextGoals;


}