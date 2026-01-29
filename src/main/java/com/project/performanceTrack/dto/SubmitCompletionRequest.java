package com.project.performanceTrack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Submit goal completion request DTO
@Data
public class SubmitCompletionRequest {

    @NotBlank(message = "Evidence link is required")
    private String evLink;

    @NotBlank(message = "Link description is required")
    private String linkDesc;

    private String accessInstr;  // Access instructions (optional)

    private String compNotes;    // Completion notes (optional)
}