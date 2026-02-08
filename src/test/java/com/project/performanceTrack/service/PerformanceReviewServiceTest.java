package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.ManagerReviewRequest;
import com.project.performanceTrack.dto.SelfAssessmentRequest;
import com.project.performanceTrack.entity.*;
import com.project.performanceTrack.enums.*;
import com.project.performanceTrack.exception.BadRequestException;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.exception.UnauthorizedException;
import com.project.performanceTrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PerformanceReviewService.
 *
 * Tests the full review lifecycle:
 * 1. Employee submits self-assessment
 * 2. Employee can update draft
 * 3. Manager submits review
 * 4. Employee acknowledges review
 */
@ExtendWith(MockitoExtension.class)
class PerformanceReviewServiceTest {

    @Mock
    private PerformanceReviewRepository reviewRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private ReviewCycleRepository cycleRepo;

    @Mock
    private AuditLogRepository auditRepo;

    @Mock
    private PerformanceReviewGoalsRepository reviewGoalsRepo;

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PerformanceReviewService reviewService;

    private User employee;
    private User manager;
    private ReviewCycle cycle;
    private PerformanceReview review;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setUserId(2);
        manager.setName("Bob Manager");
        manager.setRole(UserRole.MANAGER);

        employee = new User();
        employee.setUserId(1);
        employee.setName("Alice Employee");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setManager(manager);

        cycle = new ReviewCycle();
        cycle.setCycleId(1);
        cycle.setTitle("Q1 2025 Review");
        cycle.setStatus(ReviewCycleStatus.ACTIVE);

        review = new PerformanceReview();
        review.setReviewId(1);
        review.setCycle(cycle);
        review.setUser(employee);
        review.setStatus(PerformanceReviewStatus.PENDING);
    }

    // ==================== getReviewsByUser() ====================

    @Test
    @DisplayName("getReviewsByUser() should return reviews for the user")
    void getReviewsByUser_ShouldReturnReviews() {
        when(reviewRepo.findByUser_UserId(1)).thenReturn(Arrays.asList(review));

        List<PerformanceReview> result = reviewService.getReviewsByUser(1);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getReviewId());
    }

    // ==================== getReviewsByCycle() ====================

    @Test
    @DisplayName("getReviewsByCycle() should return reviews for the cycle")
    void getReviewsByCycle_ShouldReturnReviews() {
        when(reviewRepo.findByCycle_CycleId(1)).thenReturn(Arrays.asList(review));

        List<PerformanceReview> result = reviewService.getReviewsByCycle(1);

        assertEquals(1, result.size());
    }

    // ==================== getReviewById() ====================

    @Test
    @DisplayName("getReviewById() should return review when found")
    void getReviewById_WithValidId_ShouldReturnReview() {
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        PerformanceReview result = reviewService.getReviewById(1);

        assertNotNull(result);
        assertEquals(1, result.getReviewId());
    }

    @Test
    @DisplayName("getReviewById() should throw ResourceNotFoundException when not found")
    void getReviewById_WithInvalidId_ShouldThrowException() {
        when(reviewRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewById(999));
    }

    // ==================== submitSelfAssessment() ====================

    @Test
    @DisplayName("submitSelfAssessment() should create review with SELF_ASSESSMENT_COMPLETED status")
    void submitSelfAssessment_NewReview_ShouldCreateSuccessfully() {
        // Arrange
        SelfAssessmentRequest req = new SelfAssessmentRequest();
        req.setCycleId(1);
        req.setSelfAssmt("I did great work this quarter");
        req.setSelfRating(4);

        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(cycleRepo.findById(1)).thenReturn(Optional.of(cycle));
        when(reviewRepo.findByCycle_CycleIdAndUser_UserId(1, 1)).thenReturn(Optional.empty());
        when(goalRepo.findByAssignedToUser_UserIdAndStatus(1, GoalStatus.COMPLETED)).thenReturn(Collections.emptyList());
        when(reviewRepo.save(any(PerformanceReview.class))).thenAnswer(invocation -> {
            PerformanceReview saved = invocation.getArgument(0);
            saved.setReviewId(10);
            return saved;
        });

        // Act
        PerformanceReview result = reviewService.submitSelfAssessment(req, 1);

        // Assert
        assertNotNull(result);
        assertEquals(PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED, result.getStatus());
        assertEquals("I did great work this quarter", result.getSelfAssessment());
        assertEquals(4, result.getEmployeeSelfRating());

        // Verify notifications were sent
        verify(notificationService).sendNotification(
                eq(manager), eq(NotificationType.SELF_ASSESSMENT_SUBMITTED),
                anyString(), eq("PerformanceReview"), anyInt(), eq("HIGH"), eq(true)
        );
    }

    @Test
    @DisplayName("submitSelfAssessment() should update existing PENDING review")
    void submitSelfAssessment_ExistingPendingReview_ShouldUpdate() {
        SelfAssessmentRequest req = new SelfAssessmentRequest();
        req.setCycleId(1);
        req.setSelfAssmt("Updated assessment");
        req.setSelfRating(3);

        // Existing review in PENDING status
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(cycleRepo.findById(1)).thenReturn(Optional.of(cycle));
        when(reviewRepo.findByCycle_CycleIdAndUser_UserId(1, 1)).thenReturn(Optional.of(review));
        when(goalRepo.findByAssignedToUser_UserIdAndStatus(1, GoalStatus.COMPLETED)).thenReturn(Collections.emptyList());
        when(reviewRepo.save(any(PerformanceReview.class))).thenReturn(review);

        PerformanceReview result = reviewService.submitSelfAssessment(req, 1);

        assertNotNull(result);
        assertEquals(PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("submitSelfAssessment() should throw when already submitted")
    void submitSelfAssessment_AlreadySubmitted_ShouldThrowException() {
        SelfAssessmentRequest req = new SelfAssessmentRequest();
        req.setCycleId(1);

        review.setStatus(PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED);
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(cycleRepo.findById(1)).thenReturn(Optional.of(cycle));
        when(reviewRepo.findByCycle_CycleIdAndUser_UserId(1, 1)).thenReturn(Optional.of(review));

        assertThrows(UnauthorizedException.class, () -> reviewService.submitSelfAssessment(req, 1));
    }

    @Test
    @DisplayName("submitSelfAssessment() should link completed goals to the review")
    void submitSelfAssessment_ShouldLinkCompletedGoals() {
        SelfAssessmentRequest req = new SelfAssessmentRequest();
        req.setCycleId(1);
        req.setSelfAssmt("Assessment");
        req.setSelfRating(4);

        Goal completedGoal = new Goal();
        completedGoal.setGoalId(100);
        completedGoal.setStatus(GoalStatus.COMPLETED);

        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(cycleRepo.findById(1)).thenReturn(Optional.of(cycle));
        when(reviewRepo.findByCycle_CycleIdAndUser_UserId(1, 1)).thenReturn(Optional.empty());
        when(goalRepo.findByAssignedToUser_UserIdAndStatus(1, GoalStatus.COMPLETED))
                .thenReturn(Arrays.asList(completedGoal));
        when(reviewRepo.save(any(PerformanceReview.class))).thenAnswer(invocation -> {
            PerformanceReview saved = invocation.getArgument(0);
            saved.setReviewId(10);
            return saved;
        });

        reviewService.submitSelfAssessment(req, 1);

        // Verify that the goal was linked to the review
        verify(reviewGoalsRepo).save(any(PerformanceReviewGoals.class));
    }

    // ==================== updateSelfAssessmentDraft() ====================

    @Test
    @DisplayName("updateSelfAssessmentDraft() should update when review is PENDING")
    void updateSelfAssessmentDraft_WithPendingStatus_ShouldUpdate() {
        SelfAssessmentRequest req = new SelfAssessmentRequest();
        req.setSelfAssmt("Updated draft");
        req.setSelfRating(3);

        review.setStatus(PerformanceReviewStatus.PENDING);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(reviewRepo.save(any(PerformanceReview.class))).thenReturn(review);

        PerformanceReview result = reviewService.updateSelfAssessmentDraft(1, req, 1);

        assertNotNull(result);
        assertEquals("Updated draft", result.getSelfAssessment());
    }

    @Test
    @DisplayName("updateSelfAssessmentDraft() should throw when not authorized")
    void updateSelfAssessmentDraft_ByWrongUser_ShouldThrowException() {
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        assertThrows(UnauthorizedException.class,
                () -> reviewService.updateSelfAssessmentDraft(1, new SelfAssessmentRequest(), 999));
    }

    @Test
    @DisplayName("updateSelfAssessmentDraft() should throw when review is already completed")
    void updateSelfAssessmentDraft_WhenCompleted_ShouldThrowException() {
        review.setStatus(PerformanceReviewStatus.COMPLETED);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> reviewService.updateSelfAssessmentDraft(1, new SelfAssessmentRequest(), 1));
    }

    // ==================== submitManagerReview() ====================

    @Test
    @DisplayName("submitManagerReview() should complete the review")
    void submitManagerReview_ShouldCompleteReview() {
        review.setStatus(PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED);

        ManagerReviewRequest req = new ManagerReviewRequest();
        req.setMgrFb("Good performance overall");
        req.setMgrRating(4);
        req.setRatingJust("Consistently met targets");
        req.setCompRec("10% raise");
        req.setNextGoals("Lead a project");

        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(reviewRepo.save(any(PerformanceReview.class))).thenReturn(review);

        PerformanceReview result = reviewService.submitManagerReview(1, req, 2);

        assertEquals(PerformanceReviewStatus.COMPLETED, result.getStatus());
        assertEquals("Good performance overall", result.getManagerFeedback());
        assertEquals(4, result.getManagerRating());
        assertNotNull(result.getReviewCompletedDate());
    }

    @Test
    @DisplayName("submitManagerReview() should throw when not authorized")
    void submitManagerReview_ByWrongManager_ShouldThrowException() {
        review.setStatus(PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        assertThrows(UnauthorizedException.class,
                () -> reviewService.submitManagerReview(1, new ManagerReviewRequest(), 999));
    }

    @Test
    @DisplayName("submitManagerReview() should throw when self-assessment not completed")
    void submitManagerReview_WithoutSelfAssessment_ShouldThrowException() {
        review.setStatus(PerformanceReviewStatus.PENDING);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> reviewService.submitManagerReview(1, new ManagerReviewRequest(), 2));
    }

    // ==================== acknowledgeReview() ====================

    @Test
    @DisplayName("acknowledgeReview() should mark review as COMPLETED_AND_ACKNOWLEDGED")
    void acknowledgeReview_ShouldAcknowledge() {
        review.setStatus(PerformanceReviewStatus.COMPLETED);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(reviewRepo.save(any(PerformanceReview.class))).thenReturn(review);

        PerformanceReview result = reviewService.acknowledgeReview(1, 1, "Thank you for the feedback");

        assertEquals(PerformanceReviewStatus.COMPLETED_AND_ACKNOWLEDGED, result.getStatus());
        assertEquals("Thank you for the feedback", result.getEmployeeResponse());
        assertNotNull(result.getAcknowledgedDate());
    }

    @Test
    @DisplayName("acknowledgeReview() should throw when review not completed")
    void acknowledgeReview_WhenNotCompleted_ShouldThrowException() {
        review.setStatus(PerformanceReviewStatus.PENDING);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> reviewService.acknowledgeReview(1, 1, "Response"));
    }

    @Test
    @DisplayName("acknowledgeReview() should throw when wrong user tries to acknowledge")
    void acknowledgeReview_ByWrongUser_ShouldThrowException() {
        review.setStatus(PerformanceReviewStatus.COMPLETED);
        when(reviewRepo.findById(1)).thenReturn(Optional.of(review));

        assertThrows(UnauthorizedException.class,
                () -> reviewService.acknowledgeReview(1, 999, "Response"));
    }
}
