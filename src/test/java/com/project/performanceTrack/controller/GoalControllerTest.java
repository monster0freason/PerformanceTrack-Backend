package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.*;
import com.project.performanceTrack.entity.Goal;
import com.project.performanceTrack.enums.GoalCategory;
import com.project.performanceTrack.enums.GoalPriority;
import com.project.performanceTrack.enums.GoalStatus;
import com.project.performanceTrack.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GoalController.
 *
 * Tests all goal-related REST endpoints.
 */
@ExtendWith(MockitoExtension.class)
class GoalControllerTest {

    @Mock
    private GoalService goalSvc;

    @InjectMocks
    private GoalController goalController;

    private Goal testGoal;
    private MockHttpServletRequest empRequest;
    private MockHttpServletRequest mgrRequest;

    @BeforeEach
    void setUp() {
        testGoal = new Goal();
        testGoal.setGoalId(1);
        testGoal.setTitle("Learn Spring Boot");
        testGoal.setStatus(GoalStatus.PENDING);
        testGoal.setPriority(GoalPriority.HIGH);
        testGoal.setCategory(GoalCategory.TECHNICAL);

        // Employee request
        empRequest = new MockHttpServletRequest();
        empRequest.setAttribute("userId", 1);
        empRequest.setAttribute("userRole", "EMPLOYEE");

        // Manager request
        mgrRequest = new MockHttpServletRequest();
        mgrRequest.setAttribute("userId", 2);
        mgrRequest.setAttribute("userRole", "MANAGER");
    }

    // ==================== createGoal() ====================

    @Test
    @DisplayName("createGoal() should return success with created goal")
    void createGoal_ShouldReturnCreatedGoal() {
        CreateGoalRequest req = new CreateGoalRequest();
        req.setTitle("New Goal");

        when(goalSvc.createGoal(any(CreateGoalRequest.class), eq(1))).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.createGoal(req, empRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Goal created", response.getMsg());
        assertNotNull(response.getData());
    }

    // ==================== getGoals() ====================

    @Test
    @DisplayName("getGoals() as EMPLOYEE should return own goals")
    void getGoals_AsEmployee_ShouldReturnOwnGoals() {
        Page<Goal> goalPage = new PageImpl<>(Arrays.asList(testGoal));
        when(goalSvc.getGoalsByUser(eq(1), any(Pageable.class))).thenReturn(goalPage);

        ApiResponse<PageResponse<Goal>> response = goalController.getGoals(empRequest, null, null, 0, 20);

        assertEquals("success", response.getStatus());
        assertEquals("Goals retrieved", response.getMsg());
    }

    @Test
    @DisplayName("getGoals() as MANAGER should return managed goals")
    void getGoals_AsManager_ShouldReturnManagedGoals() {
        Page<Goal> goalPage = new PageImpl<>(Arrays.asList(testGoal));
        when(goalSvc.getGoalsByManager(eq(2), any(Pageable.class))).thenReturn(goalPage);

        ApiResponse<PageResponse<Goal>> response = goalController.getGoals(mgrRequest, null, null, 0, 20);

        assertEquals("success", response.getStatus());
    }

    @Test
    @DisplayName("getGoals() as MANAGER with userId should return that user's goals")
    void getGoals_AsManagerWithUserId_ShouldReturnUserGoals() {
        Page<Goal> goalPage = new PageImpl<>(Arrays.asList(testGoal));
        when(goalSvc.getGoalsByUser(eq(5), any(Pageable.class))).thenReturn(goalPage);

        ApiResponse<PageResponse<Goal>> response = goalController.getGoals(mgrRequest, 5, null, 0, 20);

        assertEquals("success", response.getStatus());
    }

    // ==================== getGoalById() ====================

    @Test
    @DisplayName("getGoalById() should return goal")
    void getGoalById_ShouldReturnGoal() {
        when(goalSvc.getGoalById(1)).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.getGoalById(1);

        assertEquals("success", response.getStatus());
        assertEquals("Learn Spring Boot", response.getData().getTitle());
    }

    // ==================== approveGoal() ====================

    @Test
    @DisplayName("approveGoal() should return success")
    void approveGoal_ShouldReturnSuccess() {
        when(goalSvc.approveGoal(1, 2)).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.approveGoal(1, mgrRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Goal approved", response.getMsg());
    }

    // ==================== requestChanges() ====================

    @Test
    @DisplayName("requestChanges() should return success")
    void requestChanges_ShouldReturnSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("comments", "Please revise");
        when(goalSvc.requestChanges(1, 2, "Please revise")).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.requestChanges(1, body, mgrRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Change request sent", response.getMsg());
    }

    // ==================== submitCompletion() ====================

    @Test
    @DisplayName("submitCompletion() should return success")
    void submitCompletion_ShouldReturnSuccess() {
        SubmitCompletionRequest req = new SubmitCompletionRequest();
        when(goalSvc.submitCompletion(eq(1), any(SubmitCompletionRequest.class), eq(1)))
                .thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.submitCompletion(1, req, empRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Completion submitted", response.getMsg());
    }

    // ==================== approveCompletion() ====================

    @Test
    @DisplayName("approveCompletion() should return success")
    void approveCompletion_ShouldReturnSuccess() {
        ApproveCompletionRequest req = new ApproveCompletionRequest();
        when(goalSvc.approveCompletion(eq(1), any(ApproveCompletionRequest.class), eq(2)))
                .thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.approveCompletion(1, req, mgrRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Completion approved", response.getMsg());
    }

    // ==================== requestEvidence() ====================

    @Test
    @DisplayName("requestEvidence() should return success")
    void requestEvidence_ShouldReturnSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("reason", "Need more proof");
        when(goalSvc.requestAdditionalEvidence(1, 2, "Need more proof")).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.requestEvidence(1, body, mgrRequest);

        assertEquals("success", response.getStatus());
    }

    // ==================== updateGoal() ====================

    @Test
    @DisplayName("updateGoal() should return success")
    void updateGoal_ShouldReturnSuccess() {
        CreateGoalRequest req = new CreateGoalRequest();
        when(goalSvc.updateGoal(eq(1), any(CreateGoalRequest.class), eq(1))).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.updateGoal(1, req, empRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Goal updated", response.getMsg());
    }

    // ==================== deleteGoal() ====================

    @Test
    @DisplayName("deleteGoal() should return success")
    void deleteGoal_ShouldReturnSuccess() {
        ApiResponse<Void> response = goalController.deleteGoal(1, empRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Goal deleted", response.getMsg());
        verify(goalSvc).deleteGoal(1, 1, "EMPLOYEE");
    }

    // ==================== verifyEvidence() ====================

    @Test
    @DisplayName("verifyEvidence() should return success")
    void verifyEvidence_ShouldReturnSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("status", "VERIFIED");
        body.put("notes", "Looks good");
        when(goalSvc.verifyEvidence(1, 2, "VERIFIED", "Looks good")).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.verifyEvidence(1, body, mgrRequest);

        assertEquals("success", response.getStatus());
    }

    // ==================== rejectCompletion() ====================

    @Test
    @DisplayName("rejectCompletion() should return success")
    void rejectCompletion_ShouldReturnSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("reason", "Insufficient evidence");
        when(goalSvc.rejectCompletion(1, 2, "Insufficient evidence")).thenReturn(testGoal);

        ApiResponse<Goal> response = goalController.rejectCompletion(1, body, mgrRequest);

        assertEquals("success", response.getStatus());
    }

    // ==================== addProgress() ====================

    @Test
    @DisplayName("addProgress() should return success")
    void addProgress_ShouldReturnSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("note", "Completed step 1");

        ApiResponse<Void> response = goalController.addProgress(1, body, empRequest);

        assertEquals("success", response.getStatus());
        verify(goalSvc).addProgressUpdate(1, 1, "Completed step 1");
    }

    // ==================== getProgress() ====================

    @Test
    @DisplayName("getProgress() should return progress data")
    void getProgress_ShouldReturnProgress() {
        when(goalSvc.getProgressUpdates(1)).thenReturn("Some progress notes");

        ApiResponse<String> response = goalController.getProgress(1);

        assertEquals("success", response.getStatus());
        assertEquals("Some progress notes", response.getData());
    }
}
