package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.CreateReviewCycleRequest;
import com.project.performanceTrack.entity.ReviewCycle;
import com.project.performanceTrack.service.ReviewCycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-cycles")
@RequiredArgsConstructor
public class ReviewCycleController {

    private final ReviewCycleService cycleSvc;

    //get all review cycles
    @GetMapping
    public ApiResponse<List<ReviewCycle>> getAllCycles(){
        List<ReviewCycle> cycles = cycleSvc.getAllCycles();
        return ApiResponse.success("Review cycles retrieved", cycles);
    }

    //get cycle by id
    @GetMapping("/{cycleId}")
    public ApiResponse<ReviewCycle> getCycleById(@PathVariable Integer cycleId) {
        ReviewCycle cycle = cycleSvc.getCycleById(cycleId);
        return ApiResponse.success("Review cycle retrieved", cycle);
    }

    // Get active cycle
    @GetMapping("/active")
    public ApiResponse<ReviewCycle> getActiveCycle() {
        ReviewCycle cycle = cycleSvc.getActiveCycle();
        return ApiResponse.success("Active cycle retrieved", cycle);
    }

    //create review cycle (Admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewCycle> createCycle(@Valid @RequestBody CreateReviewCycleRequest req,
                                                HttpServletRequest httpReq) {
        Integer adminId = (Integer) httpReq.getAttribute("userId");
        ReviewCycle cycle = cycleSvc.createCycle(req, adminId);
        return ApiResponse.success("Review cycle created", cycle);
    }
    // Update review cycle (Admin)
    @PutMapping("/{cycleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewCycle> updateCycle(@PathVariable Integer cycleId,
                                                @Valid @RequestBody CreateReviewCycleRequest req,
                                                HttpServletRequest httpReq) {
        Integer adminId = (Integer) httpReq.getAttribute("userId");
        ReviewCycle cycle = cycleSvc.updateCycle(cycleId, req, adminId);
        return ApiResponse.success("Review cycle updated", cycle);
    }

}