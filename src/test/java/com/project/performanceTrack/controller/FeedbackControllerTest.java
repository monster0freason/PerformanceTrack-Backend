package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.FeedbackRequest;
import com.project.performanceTrack.dto.FeedbackResponseDTO;
import com.project.performanceTrack.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedbackController.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackController feedbackController;

    private FeedbackResponseDTO testResponse;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        testResponse = new FeedbackResponseDTO();
        testResponse.setFeedbackId(1);
        testResponse.setComments("Great work");
        testResponse.setFeedbackType("POSITIVE");
        testResponse.setGiverId(1);
        testResponse.setGiverName("John");
        testResponse.setDate(LocalDateTime.now());

        mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("userId", 1);
    }

    @Test
    @DisplayName("getFeedback() should return feedback list")
    void getFeedback_ShouldReturnFeedbackList() {
        when(feedbackService.getFilteredFeedback(null, null))
                .thenReturn(Arrays.asList(testResponse));

        ApiResponse<List<FeedbackResponseDTO>> response =
                feedbackController.getFeedback(null, null);

        assertEquals("success", response.getStatus());
        assertEquals("Feedback retrieved", response.getMsg());
        assertEquals(1, response.getData().size());
    }

    @Test
    @DisplayName("getFeedback() with goalId filter should pass filter to service")
    void getFeedback_WithGoalFilter_ShouldPassFilter() {
        when(feedbackService.getFilteredFeedback(10, null))
                .thenReturn(Arrays.asList(testResponse));

        ApiResponse<List<FeedbackResponseDTO>> response =
                feedbackController.getFeedback(10, null);

        assertEquals("success", response.getStatus());
        verify(feedbackService).getFilteredFeedback(10, null);
    }

    @Test
    @DisplayName("getFeedback() should return empty list when no feedback exists")
    void getFeedback_NoData_ShouldReturnEmptyList() {
        when(feedbackService.getFilteredFeedback(null, null))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<FeedbackResponseDTO>> response =
                feedbackController.getFeedback(null, null);

        assertEquals("success", response.getStatus());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    @DisplayName("createFeedback() should return created feedback")
    void createFeedback_ShouldReturnCreatedFeedback() {
        FeedbackRequest request = new FeedbackRequest();
        request.setComments("Nice job");
        request.setFeedbackType("POSITIVE");
        request.setGoalId(10);

        when(feedbackService.saveFeedback(eq(1), any(FeedbackRequest.class)))
                .thenReturn(testResponse);

        ApiResponse<FeedbackResponseDTO> response =
                feedbackController.createFeedback(request, mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Feedback created", response.getMsg());
        assertNotNull(response.getData());
    }
}
