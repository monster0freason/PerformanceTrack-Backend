package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.FeedbackRequest;
import com.project.performanceTrack.dto.FeedbackResponseDTO;
import com.project.performanceTrack.entity.*;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedbackService.
 *
 * Tests feedback retrieval (with filters) and feedback creation.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository fbRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private PerformanceReviewRepository reviewRepo;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private FeedbackService feedbackService;

    private User testUser;
    private Feedback testFeedback;
    private FeedbackResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setRole(UserRole.EMPLOYEE);

        testFeedback = new Feedback();
        testFeedback.setFeedbackId(1);
        testFeedback.setGivenByUser(testUser);
        testFeedback.setComments("Great progress!");
        testFeedback.setFeedbackType("POSITIVE");
        testFeedback.setDate(LocalDateTime.now());

        testResponseDTO = new FeedbackResponseDTO();
        testResponseDTO.setFeedbackId(1);
        testResponseDTO.setComments("Great progress!");
        testResponseDTO.setFeedbackType("POSITIVE");
        testResponseDTO.setGiverId(1);
        testResponseDTO.setGiverName("John Doe");
    }

    // ==================== getFilteredFeedback() ====================

    @Test
    @DisplayName("getFilteredFeedback() should filter by goalId when provided")
    void getFilteredFeedback_ByGoalId_ShouldFilterByGoal() {
        when(fbRepo.findByGoal_GoalId(10)).thenReturn(Arrays.asList(testFeedback));
        when(modelMapper.map(testFeedback, FeedbackResponseDTO.class)).thenReturn(testResponseDTO);

        List<FeedbackResponseDTO> result = feedbackService.getFilteredFeedback(10, null);

        assertEquals(1, result.size());
        assertEquals("Great progress!", result.get(0).getComments());
        verify(fbRepo).findByGoal_GoalId(10);
    }

    @Test
    @DisplayName("getFilteredFeedback() should filter by reviewId when provided")
    void getFilteredFeedback_ByReviewId_ShouldFilterByReview() {
        when(fbRepo.findByReview_ReviewId(5)).thenReturn(Arrays.asList(testFeedback));
        when(modelMapper.map(testFeedback, FeedbackResponseDTO.class)).thenReturn(testResponseDTO);

        List<FeedbackResponseDTO> result = feedbackService.getFilteredFeedback(null, 5);

        assertEquals(1, result.size());
        verify(fbRepo).findByReview_ReviewId(5);
    }

    @Test
    @DisplayName("getFilteredFeedback() should return all when no filters provided")
    void getFilteredFeedback_NoFilters_ShouldReturnAll() {
        when(fbRepo.findAll()).thenReturn(Arrays.asList(testFeedback));
        when(modelMapper.map(testFeedback, FeedbackResponseDTO.class)).thenReturn(testResponseDTO);

        List<FeedbackResponseDTO> result = feedbackService.getFilteredFeedback(null, null);

        assertEquals(1, result.size());
        verify(fbRepo).findAll();
    }

    @Test
    @DisplayName("getFilteredFeedback() should return empty list when no feedback exists")
    void getFilteredFeedback_NoData_ShouldReturnEmptyList() {
        when(fbRepo.findAll()).thenReturn(Collections.emptyList());

        List<FeedbackResponseDTO> result = feedbackService.getFilteredFeedback(null, null);

        assertTrue(result.isEmpty());
    }

    // ==================== saveFeedback() ====================

    @Test
    @DisplayName("saveFeedback() should create feedback linked to a goal")
    void saveFeedback_WithGoalId_ShouldCreateFeedbackForGoal() {
        FeedbackRequest request = new FeedbackRequest();
        request.setComments("Good job");
        request.setFeedbackType("POSITIVE");
        request.setGoalId(10);

        Goal goal = new Goal();
        goal.setGoalId(10);

        Feedback mappedFeedback = new Feedback();
        mappedFeedback.setComments("Good job");
        mappedFeedback.setFeedbackType("POSITIVE");

        Feedback savedFeedback = new Feedback();
        savedFeedback.setFeedbackId(100);
        savedFeedback.setComments("Good job");

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(request, Feedback.class)).thenReturn(mappedFeedback);
        when(goalRepo.findById(10)).thenReturn(Optional.of(goal));
        when(fbRepo.save(any(Feedback.class))).thenReturn(savedFeedback);
        when(modelMapper.map(savedFeedback, FeedbackResponseDTO.class)).thenReturn(testResponseDTO);

        FeedbackResponseDTO result = feedbackService.saveFeedback(1, request);

        assertNotNull(result);
        verify(goalRepo).findById(10);
        verify(auditLogService).logAudit(eq(testUser), eq("FEEDBACK_CREATED"),
                anyString(), eq("Feedback"), anyInt(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("saveFeedback() should create feedback linked to a review")
    void saveFeedback_WithReviewId_ShouldCreateFeedbackForReview() {
        FeedbackRequest request = new FeedbackRequest();
        request.setComments("Review feedback");
        request.setFeedbackType("CONSTRUCTIVE");
        request.setReviewId(5);

        PerformanceReview review = new PerformanceReview();
        review.setReviewId(5);

        Feedback mappedFeedback = new Feedback();
        Feedback savedFeedback = new Feedback();
        savedFeedback.setFeedbackId(101);

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(request, Feedback.class)).thenReturn(mappedFeedback);
        when(reviewRepo.findById(5)).thenReturn(Optional.of(review));
        when(fbRepo.save(any(Feedback.class))).thenReturn(savedFeedback);
        when(modelMapper.map(savedFeedback, FeedbackResponseDTO.class)).thenReturn(testResponseDTO);

        FeedbackResponseDTO result = feedbackService.saveFeedback(1, request);

        assertNotNull(result);
        verify(reviewRepo).findById(5);
    }

    @Test
    @DisplayName("saveFeedback() should throw when user not found")
    void saveFeedback_WithInvalidUser_ShouldThrowException() {
        FeedbackRequest request = new FeedbackRequest();
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> feedbackService.saveFeedback(999, request));
    }

    @Test
    @DisplayName("saveFeedback() should throw when goal not found")
    void saveFeedback_WithInvalidGoal_ShouldThrowException() {
        FeedbackRequest request = new FeedbackRequest();
        request.setGoalId(999);

        Feedback mappedFeedback = new Feedback();

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(request, Feedback.class)).thenReturn(mappedFeedback);
        when(goalRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> feedbackService.saveFeedback(1, request));
    }
}
