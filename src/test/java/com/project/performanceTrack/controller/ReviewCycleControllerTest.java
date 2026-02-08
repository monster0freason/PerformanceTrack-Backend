package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.CreateReviewCycleRequest;
import com.project.performanceTrack.entity.ReviewCycle;
import com.project.performanceTrack.enums.ReviewCycleStatus;
import com.project.performanceTrack.service.ReviewCycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewCycleController.
 */
@ExtendWith(MockitoExtension.class)
class ReviewCycleControllerTest {

    @Mock
    private ReviewCycleService cycleSvc;

    @InjectMocks
    private ReviewCycleController cycleController;

    private ReviewCycle testCycle;
    private MockHttpServletRequest adminRequest;

    @BeforeEach
    void setUp() {
        testCycle = new ReviewCycle();
        testCycle.setCycleId(1);
        testCycle.setTitle("Q1 2025 Review");
        testCycle.setStatus(ReviewCycleStatus.ACTIVE);
        testCycle.setStartDate(LocalDate.of(2025, 1, 1));
        testCycle.setEndDate(LocalDate.of(2025, 3, 31));

        adminRequest = new MockHttpServletRequest();
        adminRequest.setAttribute("userId", 100);
    }

    @Test
    @DisplayName("getAllCycles() should return all review cycles")
    void getAllCycles_ShouldReturnAllCycles() {
        when(cycleSvc.getAllCycles()).thenReturn(Arrays.asList(testCycle));

        ApiResponse<List<ReviewCycle>> response = cycleController.getAllCycles();

        assertEquals("success", response.getStatus());
        assertEquals("Review cycles retrieved", response.getMsg());
        assertEquals(1, response.getData().size());
    }

    @Test
    @DisplayName("getCycleById() should return cycle")
    void getCycleById_ShouldReturnCycle() {
        when(cycleSvc.getCycleById(1)).thenReturn(testCycle);

        ApiResponse<ReviewCycle> response = cycleController.getCycleById(1);

        assertEquals("success", response.getStatus());
        assertEquals("Q1 2025 Review", response.getData().getTitle());
    }

    @Test
    @DisplayName("getActiveCycle() should return active cycle")
    void getActiveCycle_ShouldReturnActiveCycle() {
        when(cycleSvc.getActiveCycle()).thenReturn(testCycle);

        ApiResponse<ReviewCycle> response = cycleController.getActiveCycle();

        assertEquals("success", response.getStatus());
        assertEquals(ReviewCycleStatus.ACTIVE, response.getData().getStatus());
    }

    @Test
    @DisplayName("createCycle() should return created cycle")
    void createCycle_ShouldReturnCreatedCycle() {
        CreateReviewCycleRequest req = new CreateReviewCycleRequest();
        req.setTitle("New Cycle");
        req.setStatus(ReviewCycleStatus.ACTIVE);

        when(cycleSvc.createCycle(any(CreateReviewCycleRequest.class), eq(100))).thenReturn(testCycle);

        ApiResponse<ReviewCycle> response = cycleController.createCycle(req, adminRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Review cycle created", response.getMsg());
    }

    @Test
    @DisplayName("updateCycle() should return updated cycle")
    void updateCycle_ShouldReturnUpdatedCycle() {
        CreateReviewCycleRequest req = new CreateReviewCycleRequest();
        req.setTitle("Updated Cycle");

        when(cycleSvc.updateCycle(eq(1), any(CreateReviewCycleRequest.class), eq(100)))
                .thenReturn(testCycle);

        ApiResponse<ReviewCycle> response = cycleController.updateCycle(1, req, adminRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Review cycle updated", response.getMsg());
    }
}
