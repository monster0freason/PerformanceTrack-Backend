package com.project.performanceTrack.dto;

import lombok.Data;

@Data
public class FeedbackRequest {
    private String comments;
    private String feedbackType;
    private Integer goalId;
    private Integer reviewId;
}