package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.entity.Feedback;
import com.project.performanceTrack.entity.Goal;
import com.project.performanceTrack.entity.PerformanceReview;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.repository.FeedbackRepository;
import com.project.performanceTrack.repository.GoalRepository;
import com.project.performanceTrack.repository.PerformanceReviewRepository;
import com.project.performanceTrack.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackRepository fbRepo;

    @Autowired
    private GoalRepository goalRepo;

    @Autowired
    private PerformanceReviewRepository reviewRepo;

    @Autowired
    private UserRepository userRepo;

    // Get feedback with filters
    @GetMapping
    public ApiResponse<List<Feedback>> getFeedback(@RequestParam(required = false) Integer goalId,
                                                   @RequestParam(required = false) Integer reviewId) {
        List<Feedback> feedback;
        if (goalId != null) {
            feedback = fbRepo.findByGoal_GoalId(goalId);
        } else if (reviewId != null) {
            feedback = fbRepo.findByReview_ReviewId(reviewId);
        } else {
            feedback = fbRepo.findAll();
        }
        return ApiResponse.success("Feedback retrieved", feedback);
    }

    // Create feedback
    @PostMapping
    public ApiResponse<Feedback> createFeedback(@RequestBody Map<String, Object> body,
                                                HttpServletRequest httpReq) {
        Integer userId = (Integer) httpReq.getAttribute("userId");
        User user = userRepo.findById(userId).orElse(null);

        Feedback fb = new Feedback();
        fb.setGivenByUser(user);
        fb.setComments((String) body.get("comments"));
        fb.setFeedbackType((String) body.get("feedbackType"));
        fb.setDate(LocalDateTime.now());

        // Link to goal or review if provided
        if (body.get("goalId") != null) {
            Integer goalId = (Integer) body.get("goalId");
            Goal goal = goalRepo.findById(goalId).orElse(null);
            fb.setGoal(goal);
        }

        if (body.get("reviewId") != null) {
            Integer reviewId = (Integer) body.get("reviewId");
            PerformanceReview review = reviewRepo.findById(reviewId).orElse(null);
            fb.setReview(review);
        }

        Feedback saved = fbRepo.save(fb);
        return ApiResponse.success("Feedback created", saved);
    }
}