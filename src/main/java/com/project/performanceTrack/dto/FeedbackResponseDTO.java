package com.project.performanceTrack.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeedbackResponseDTO {
    private Integer feedbackId;
    private String comments;
    private String feedbackType;
    private LocalDateTime date;

    // Flattened User Data
    private Integer giverId;
    private String giverName;

    // Flattened Goal Data (Optional)
    private String goalTitle;
    private Integer goalId;
}