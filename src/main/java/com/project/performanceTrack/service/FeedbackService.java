package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.*;
import com.project.performanceTrack.entity.*;
import com.project.performanceTrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor // Automatically injects all final fields
public class FeedbackService {

    private final FeedbackRepository fbRepo;
    private final UserRepository userRepo;
    private final GoalRepository goalRepo;
    private final PerformanceReviewRepository reviewRepo;
    private final ModelMapper modelMapper;

    public List<FeedbackResponseDTO> getFilteredFeedback(Integer goalId, Integer reviewId) {
        List<Feedback> feedbackList;
        if (goalId != null) feedbackList = fbRepo.findByGoal_GoalId(goalId);
        else if (reviewId != null) feedbackList = fbRepo.findByReview_ReviewId(reviewId);
        else feedbackList = fbRepo.findAll();

        return feedbackList.stream()
                .map(fb -> modelMapper.map(fb, FeedbackResponseDTO.class))
                .toList();
    }

    public FeedbackResponseDTO saveFeedback(Integer userId, FeedbackRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Map request to entity, then manually set the specific relations
        Feedback fb = modelMapper.map(request, Feedback.class);
        fb.setGivenByUser(user);
        fb.setDate(LocalDateTime.now());

        if (request.getGoalId() != null) {
            Goal goal = goalRepo.findById(request.getGoalId())
                    //earlier was using ifPresent instead of orElseThrow, it caused orphan data insertion.
                    .orElseThrow(() -> new RuntimeException("Goal not found with ID " + request.getGoalId()));
            fb.setGoal(goal);
        }
        if (request.getReviewId() != null) {
            PerformanceReview review = reviewRepo.findById(request.getReviewId())
                    .orElseThrow(() -> new RuntimeException("Review not found with ID " + request.getReviewId()));
            fb.setReview(review);
        }

        return modelMapper.map(fbRepo.save(fb), FeedbackResponseDTO.class);
    }
}