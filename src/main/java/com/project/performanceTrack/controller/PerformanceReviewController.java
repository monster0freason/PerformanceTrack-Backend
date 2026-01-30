package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.ManagerReviewRequest;
import com.project.performanceTrack.dto.SelfAssessmentRequest;
import com.project.performanceTrack.entity.PerformanceReview;
import com.project.performanceTrack.service.PerformanceReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/performance-reviews")
public class PerformanceReviewController {
    @Autowired
    private PerformanceReviewService reviewSvc;

    //get reviews
    @GetMapping
    public ApiResponse<List<PerformanceReview>> getReviews(HttpServletRequest httpReq,
                                                           @RequestParam(required = false) Integer userId,
                                                           @RequestParam(required = false) Integer cycleId) {
        String role = (String) httpReq.getAttribute("userRole");
        Integer currentUserId = (Integer) httpReq.getAttribute("userId");


        List<PerformanceReview> reviews;
        if (cycleId != null) {
            reviews = reviewSvc.getReviewsByCycle(cycleId);
        }else if(userId != null && role.equals("ADMIN")){
            reviews = reviewSvc.getReviewsByUser(userId);
        }else{
            reviews = reviewSvc.getReviewsByUser(currentUserId);
        }

        return ApiResponse.success("Reviews retrieved", reviews);
    }

    //get review by ID
    @GetMapping("/{reviewId}")
    public  ApiResponse<PerformanceReview> getReviewById(@PathVariable Integer reviewId){
        PerformanceReview review = reviewSvc.getReviewById(reviewId);
        return ApiResponse.success("Review retrieved", review);

    }

    // Update self-assessment draft (Employee)
    @PutMapping("/{reviewId}/draft")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<PerformanceReview> updateDraft(@PathVariable Integer reviewId,
                                                      @Valid @RequestBody SelfAssessmentRequest req,
                                                      HttpServletRequest httpReq){
        Integer empId = (Integer) httpReq.getAttribute("userId");
        PerformanceReview review = reviewSvc.updateSelfAssessmentDraft(reviewId, req, empId);
        return ApiResponse.success("Draft updated", review);

    }


    //submit manager review (Manager)
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<PerformanceReview> submitManagerReview(@PathVariable Integer reviewId,
                                                              @Valid @RequestBody ManagerReviewRequest req,
                                                              HttpServletRequest httpReq){
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        PerformanceReview review = reviewSvc.submitManagerReview(reviewId, req, mgrId);
        return ApiResponse.success("Manager review submitted", review);
    }


}