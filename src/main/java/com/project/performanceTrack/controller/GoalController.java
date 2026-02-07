package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.*;
import com.project.performanceTrack.entity.Goal;
import com.project.performanceTrack.service.GoalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Goal management controller
@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalSvc;

    // Create goal (Employee)
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<Goal> createGoal(@Valid @RequestBody CreateGoalRequest req,
                                        HttpServletRequest httpReq) {
        Integer empId = (Integer) httpReq.getAttribute("userId");
        Goal goal = goalSvc.createGoal(req, empId);
        return ApiResponse.success("Goal created", goal);
    }

    // Get goals by user (Employee)
    @GetMapping
    public ApiResponse<PageResponse<Goal>> getGoals(
            HttpServletRequest httpReq,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer mgrId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String role = (String) httpReq.getAttribute("userRole");
        Integer currentUserId = (Integer) httpReq.getAttribute("userId");

        // Cap page size at 100 to prevent abuse
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdDate").descending());

        Page<Goal> goals;
        if (role.equals("EMPLOYEE")) {
            goals = goalSvc.getGoalsByUser(currentUserId, pageable);
        } else if (role.equals("MANAGER")) {
            if (userId != null) {
                goals = goalSvc.getGoalsByUser(userId, pageable);
            } else {
                goals = goalSvc.getGoalsByManager(currentUserId, pageable);
            }
        } else {
            goals = userId != null ? goalSvc.getGoalsByUser(userId, pageable) :
                    mgrId != null ? goalSvc.getGoalsByManager(mgrId, pageable) :
                            goalSvc.getGoalsByUser(currentUserId, pageable);
        }

        return ApiResponse.successPage("Goals retrieved", goals);
    }

    // Get goal by ID
    @GetMapping("/{goalId}")
    public ApiResponse<Goal> getGoalById(@PathVariable Integer goalId) {
        Goal goal = goalSvc.getGoalById(goalId);
        return ApiResponse.success("Goal retrieved", goal);
    }

    // Approve goal (Manager)
    @PutMapping("/{goalId}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<Goal> approveGoal(@PathVariable Integer goalId,
                                         HttpServletRequest httpReq) {
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        Goal goal = goalSvc.approveGoal(goalId, mgrId);
        return ApiResponse.success("Goal approved", goal);
    }

    // Request changes (Manager)
    @PutMapping("/{goalId}/request-changes")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<Goal> requestChanges(@PathVariable Integer goalId,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest httpReq) {
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        String comments = body.get("comments");
        Goal goal = goalSvc.requestChanges(goalId, mgrId, comments);
        return ApiResponse.success("Change request sent", goal);
    }

    // Submit completion (Employee)
    @PostMapping("/{goalId}/submit-completion")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<Goal> submitCompletion(@PathVariable Integer goalId,
                                              @Valid @RequestBody SubmitCompletionRequest req,
                                              HttpServletRequest httpReq) {
        Integer empId = (Integer) httpReq.getAttribute("userId");
        Goal goal = goalSvc.submitCompletion(goalId, req, empId);
        return ApiResponse.success("Completion submitted", goal);
    }

    // Approve completion (Manager)
    @PostMapping("/{goalId}/approve-completion")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<Goal> approveCompletion(@PathVariable Integer goalId,
                                               @RequestBody ApproveCompletionRequest req,
                                               HttpServletRequest httpReq) {
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        Goal goal = goalSvc.approveCompletion(goalId, req, mgrId);
        return ApiResponse.success("Completion approved", goal);
    }

    // Request additional evidence (Manager)
    @PostMapping("/{goalId}/request-additional-evidence")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<Goal> requestEvidence(@PathVariable Integer goalId,
                                             @RequestBody Map<String, String> body,
                                             HttpServletRequest httpReq) {
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        String reason = body.get("reason");
        Goal goal = goalSvc.requestAdditionalEvidence(goalId, mgrId, reason);
        return ApiResponse.success("Additional evidence requested", goal);
    }

    // Update goal (Employee - only when changes requested)
    @PutMapping("/{goalId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<Goal> updateGoal(@PathVariable Integer goalId,
                                        @Valid @RequestBody CreateGoalRequest req,
                                        HttpServletRequest httpReq) {
        Integer empId = (Integer) httpReq.getAttribute("userId");
        Goal goal = goalSvc.updateGoal(goalId, req, empId);
        return ApiResponse.success("Goal updated", goal);
    }

    // Delete goal (soft delete)
    @DeleteMapping("/{goalId}")
    public ApiResponse<Void> deleteGoal(@PathVariable Integer goalId,
                                        HttpServletRequest httpReq) {
        Integer userId = (Integer) httpReq.getAttribute("userId");
        String role = (String) httpReq.getAttribute("userRole");
        goalSvc.deleteGoal(goalId, userId, role);
        return ApiResponse.success("Goal deleted");
    }

    // Verify evidence (Manager)
    @PutMapping("/{goalId}/evidence/verify")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<Goal> verifyEvidence(@PathVariable Integer goalId,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest httpReq) {
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        String status = body.get("status");
        String notes = body.get("notes");
        Goal goal = goalSvc.verifyEvidence(goalId, mgrId, status, notes);
        return ApiResponse.success("Evidence verified", goal);
    }

    // Reject goal completion (Manager)
    @PostMapping("/{goalId}/reject-completion")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<Goal> rejectCompletion(@PathVariable Integer goalId,
                                              @RequestBody Map<String, String> body,
                                              HttpServletRequest httpReq) {
        Integer mgrId = (Integer) httpReq.getAttribute("userId");
        String reason = body.get("reason");
        Goal goal = goalSvc.rejectCompletion(goalId, mgrId, reason);
        return ApiResponse.success("Goal completion rejected", goal);
    }

    // Add progress update (Employee)
    @PostMapping("/{goalId}/progress")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<Void> addProgress(@PathVariable Integer goalId,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest httpReq) {
        Integer empId = (Integer) httpReq.getAttribute("userId");
        String progressNote = body.get("note");
        goalSvc.addProgressUpdate(goalId, empId, progressNote);
        return ApiResponse.success("Progress added");
    }

    // Get progress updates
    @GetMapping("/{goalId}/progress")
    public ApiResponse<String> getProgress(@PathVariable Integer goalId) {
        String progress = goalSvc.getProgressUpdates(goalId);
        return ApiResponse.success("Progress retrieved", progress);
    }
}