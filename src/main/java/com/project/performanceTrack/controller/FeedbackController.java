package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.FeedbackRequest;
import com.project.performanceTrack.dto.FeedbackResponseDTO;
import com.project.performanceTrack.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    /**
     * Fetches feedback entries with optional filtering by goal or review ID.
     */
    @GetMapping
    public ApiResponse<List<FeedbackResponseDTO>> getFeedback(
            @RequestParam(required = false) Integer goalId,
            @RequestParam(required = false) Integer reviewId) {

        return ApiResponse.success("Feedback retrieved", feedbackService.getFilteredFeedback(goalId, reviewId));
    }

    /**
     * Creates a new feedback entry using the request body and the authenticated user ID from the request attributes.
     */
    @PostMapping
    public ApiResponse<FeedbackResponseDTO> createFeedback(
            @RequestBody FeedbackRequest request,
            HttpServletRequest httpReq) {

        Integer userId = (Integer) httpReq.getAttribute("userId");
        return ApiResponse.success("Feedback created", feedbackService.saveFeedback(userId, request));
    }
}