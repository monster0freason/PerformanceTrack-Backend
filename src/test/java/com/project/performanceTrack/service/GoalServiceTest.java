package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.ApproveCompletionRequest;
import com.project.performanceTrack.dto.CreateGoalRequest;
import com.project.performanceTrack.dto.SubmitCompletionRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GoalService - the biggest service in the project.
 *
 * This covers the full goal lifecycle:
 * 1. Employee creates a goal -> PENDING
 * 2. Manager approves it -> IN_PROGRESS
 * 3. Employee submits completion -> PENDING_COMPLETION_APPROVAL
 * 4. Manager approves completion -> COMPLETED
 *
 * Also covers: request changes, update goal, delete goal, verify evidence,
 * reject completion, add progress updates.
 */
@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditLogRepository auditRepo;

    @Mock
    private FeedbackRepository fbRepo;

    @Mock
    private GoalCompletionApprovalRepository approvalRepo;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GoalService goalService;

    // Shared test data
    private User employee;
    private User manager;
    private Goal testGoal;

    @BeforeEach
    void setUp() {
        // Create an employee
        employee = new User();
        employee.setUserId(1);
        employee.setName("Alice Employee");
        employee.setEmail("alice@example.com");
        employee.setRole(UserRole.EMPLOYEE);

        // Create a manager
        manager = new User();
        manager.setUserId(2);
        manager.setName("Bob Manager");
        manager.setEmail("bob@example.com");
        manager.setRole(UserRole.MANAGER);

        // Create a sample goal
        testGoal = new Goal();
        testGoal.setGoalId(1);
        testGoal.setTitle("Learn Spring Boot");
        testGoal.setDescription("Complete Spring Boot course");
        testGoal.setCategory(GoalCategory.TECHNICAL);
        testGoal.setPriority(GoalPriority.HIGH);
        testGoal.setAssignedToUser(employee);
        testGoal.setAssignedManager(manager);
        testGoal.setStatus(GoalStatus.PENDING);
        testGoal.setRequestChanges(false);
    }

    // ==================== createGoal() ====================

    @Test
    @DisplayName("createGoal() should create a goal with PENDING status")
    void createGoal_WithValidRequest_ShouldCreateGoal() {
        // Arrange
        CreateGoalRequest req = new CreateGoalRequest();
        req.setTitle("Learn Spring Boot");
        req.setDesc("Complete the course");
        req.setCat(GoalCategory.TECHNICAL);
        req.setPri(GoalPriority.HIGH);
        req.setStartDt(LocalDate.now());
        req.setEndDt(LocalDate.now().plusMonths(3));
        req.setMgrId(2);

        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal saved = invocation.getArgument(0);
            saved.setGoalId(10);
            return saved;
        });

        // Act
        Goal result = goalService.createGoal(req, 1);

        // Assert
        assertNotNull(result);
        assertEquals(GoalStatus.PENDING, result.getStatus(), "New goals should be PENDING");
        assertEquals("Learn Spring Boot", result.getTitle());
        assertEquals(employee, result.getAssignedToUser());
        assertEquals(manager, result.getAssignedManager());

        // Verify notification was sent to manager
        verify(notificationService).sendNotification(
                eq(manager), eq(NotificationType.GOAL_SUBMITTED),
                anyString(), eq("Goal"), anyInt(), eq("HIGH"), eq(true)
        );
    }

    @Test
    @DisplayName("createGoal() should throw BadRequestException when end date is before start date")
    void createGoal_WithInvalidDates_ShouldThrowBadRequestException() {
        // Arrange: End date is before start date
        CreateGoalRequest req = new CreateGoalRequest();
        req.setStartDt(LocalDate.of(2025, 6, 1));
        req.setEndDt(LocalDate.of(2025, 1, 1));
        req.setMgrId(2);

        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> goalService.createGoal(req, 1)
        );

        assertEquals("End date must be after start date", exception.getMessage());
    }

    @Test
    @DisplayName("createGoal() should throw ResourceNotFoundException when employee not found")
    void createGoal_WithInvalidEmployee_ShouldThrowResourceNotFoundException() {
        // Arrange
        CreateGoalRequest req = new CreateGoalRequest();
        req.setMgrId(2);
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> goalService.createGoal(req, 999));
    }

    // ==================== getGoalsByUser() / getGoalsByManager() ====================

    @Test
    @DisplayName("getGoalsByUser() should return list of goals for the given user")
    void getGoalsByUser_ShouldReturnUserGoals() {
        // Arrange
        when(goalRepo.findByAssignedToUser_UserId(1)).thenReturn(Arrays.asList(testGoal));

        // Act
        List<Goal> result = goalService.getGoalsByUser(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Learn Spring Boot", result.get(0).getTitle());
    }

    @Test
    @DisplayName("getGoalsByUser() paginated should return page of goals")
    void getGoalsByUser_Paginated_ShouldReturnPageOfGoals() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Goal> goalPage = new PageImpl<>(Arrays.asList(testGoal), pageable, 1);
        when(goalRepo.findByAssignedToUser_UserId(1, pageable)).thenReturn(goalPage);

        // Act
        Page<Goal> result = goalService.getGoalsByUser(1, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Learn Spring Boot", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("getGoalsByManager() should return list of goals for the given manager")
    void getGoalsByManager_ShouldReturnManagerGoals() {
        // Arrange
        when(goalRepo.findByAssignedManager_UserId(2)).thenReturn(Arrays.asList(testGoal));

        // Act
        List<Goal> result = goalService.getGoalsByManager(2);

        // Assert
        assertEquals(1, result.size());
    }

    // ==================== getGoalById() ====================

    @Test
    @DisplayName("getGoalById() should return goal when found")
    void getGoalById_WithValidId_ShouldReturnGoal() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        Goal result = goalService.getGoalById(1);

        assertNotNull(result);
        assertEquals(1, result.getGoalId());
    }

    @Test
    @DisplayName("getGoalById() should throw ResourceNotFoundException when goal not found")
    void getGoalById_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(goalRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.getGoalById(999));
    }

    // ==================== approveGoal() ====================

    @Test
    @DisplayName("approveGoal() should change status to IN_PROGRESS")
    void approveGoal_WithValidRequest_ShouldApproveGoal() {
        // Arrange
        testGoal.setStatus(GoalStatus.PENDING);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        // Act
        Goal result = goalService.approveGoal(1, 2);

        // Assert
        assertEquals(GoalStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getApprovedDate());
        assertFalse(result.getRequestChanges());

        // Verify notification to employee
        verify(notificationService).sendNotification(
                eq(employee), eq(NotificationType.GOAL_APPROVED),
                anyString(), eq("Goal"), eq(1), eq("HIGH"), eq(false)
        );
    }

    @Test
    @DisplayName("approveGoal() should throw UnauthorizedException for wrong manager")
    void approveGoal_WithWrongManager_ShouldThrowUnauthorizedException() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        // Manager ID 999 is not the assigned manager
        assertThrows(UnauthorizedException.class, () -> goalService.approveGoal(1, 999));
    }

    @Test
    @DisplayName("approveGoal() should throw BadRequestException when goal is not PENDING")
    void approveGoal_WithNonPendingGoal_ShouldThrowBadRequestException() {
        testGoal.setStatus(GoalStatus.IN_PROGRESS); // Not PENDING
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(BadRequestException.class, () -> goalService.approveGoal(1, 2));
    }

    // ==================== requestChanges() ====================

    @Test
    @DisplayName("requestChanges() should mark goal as needing changes")
    void requestChanges_WithValidRequest_ShouldRequestChanges() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);
        when(fbRepo.save(any(Feedback.class))).thenReturn(new Feedback());

        Goal result = goalService.requestChanges(1, 2, "Please update the description");

        assertTrue(result.getRequestChanges(), "Goal should have requestChanges = true");
        assertNotNull(result.getLastReviewedDate());

        // Verify feedback was saved
        verify(fbRepo).save(any(Feedback.class));

        // Verify notification
        verify(notificationService).sendNotification(
                eq(employee), eq(NotificationType.GOAL_CHANGE_REQUESTED),
                anyString(), eq("Goal"), eq(1), eq("NORMAL"), eq(true)
        );
    }

    @Test
    @DisplayName("requestChanges() should throw UnauthorizedException for wrong manager")
    void requestChanges_WithWrongManager_ShouldThrowUnauthorizedException() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(UnauthorizedException.class,
                () -> goalService.requestChanges(1, 999, "Comments"));
    }

    // ==================== submitCompletion() ====================

    @Test
    @DisplayName("submitCompletion() should set status to PENDING_COMPLETION_APPROVAL")
    void submitCompletion_WithValidRequest_ShouldSubmitCompletion() {
        // Arrange: Goal must be IN_PROGRESS to submit completion
        testGoal.setStatus(GoalStatus.IN_PROGRESS);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        SubmitCompletionRequest req = new SubmitCompletionRequest();
        req.setEvLink("https://github.com/project");
        req.setLinkDesc("GitHub repository");
        req.setAccessInstr("Clone the repo");
        req.setCompNotes("All tasks completed");

        // Act
        Goal result = goalService.submitCompletion(1, req, 1);

        // Assert
        assertEquals(GoalStatus.PENDING_COMPLETION_APPROVAL, result.getStatus());
        assertEquals("https://github.com/project", result.getEvidenceLink());
        assertEquals(CompletionApprovalStatus.PENDING, result.getCompletionApprovalStatus());
        assertEquals(EvidenceVerificationStatus.NOT_VERIFIED, result.getEvidenceLinkVerificationStatus());
    }

    @Test
    @DisplayName("submitCompletion() should throw UnauthorizedException for wrong employee")
    void submitCompletion_WithWrongEmployee_ShouldThrowUnauthorizedException() {
        testGoal.setStatus(GoalStatus.IN_PROGRESS);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(UnauthorizedException.class,
                () -> goalService.submitCompletion(1, new SubmitCompletionRequest(), 999));
    }

    @Test
    @DisplayName("submitCompletion() should throw BadRequestException when goal is not IN_PROGRESS")
    void submitCompletion_WithNonInProgressGoal_ShouldThrowBadRequestException() {
        testGoal.setStatus(GoalStatus.PENDING); // Not IN_PROGRESS
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(BadRequestException.class,
                () -> goalService.submitCompletion(1, new SubmitCompletionRequest(), 1));
    }

    // ==================== approveCompletion() ====================

    @Test
    @DisplayName("approveCompletion() should set status to COMPLETED")
    void approveCompletion_WithValidRequest_ShouldComplete() {
        testGoal.setStatus(GoalStatus.PENDING_COMPLETION_APPROVAL);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);
        when(approvalRepo.save(any(GoalCompletionApproval.class))).thenReturn(new GoalCompletionApproval());

        ApproveCompletionRequest req = new ApproveCompletionRequest();
        req.setMgrComments("Great work!");

        Goal result = goalService.approveCompletion(1, req, 2);

        assertEquals(GoalStatus.COMPLETED, result.getStatus());
        assertEquals(CompletionApprovalStatus.APPROVED, result.getCompletionApprovalStatus());
        assertEquals(EvidenceVerificationStatus.VERIFIED, result.getEvidenceLinkVerificationStatus());

        // Verify an approval record was saved
        verify(approvalRepo).save(any(GoalCompletionApproval.class));
    }

    @Test
    @DisplayName("approveCompletion() should throw when goal is not PENDING_COMPLETION_APPROVAL")
    void approveCompletion_WithWrongStatus_ShouldThrowBadRequestException() {
        testGoal.setStatus(GoalStatus.IN_PROGRESS);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(BadRequestException.class,
                () -> goalService.approveCompletion(1, new ApproveCompletionRequest(), 2));
    }

    // ==================== requestAdditionalEvidence() ====================

    @Test
    @DisplayName("requestAdditionalEvidence() should update approval and evidence status")
    void requestAdditionalEvidence_ShouldUpdateStatuses() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        Goal result = goalService.requestAdditionalEvidence(1, 2, "Need more proof");

        assertEquals(CompletionApprovalStatus.ADDITIONAL_EVIDENCE_REQUIRED, result.getCompletionApprovalStatus());
        assertEquals(EvidenceVerificationStatus.NEEDS_ADDITIONAL_LINK, result.getEvidenceLinkVerificationStatus());
    }

    // ==================== updateGoal() ====================

    @Test
    @DisplayName("updateGoal() should update goal when changes were requested")
    void updateGoal_WithChangesRequested_ShouldUpdateGoal() {
        testGoal.setRequestChanges(true); // Changes have been requested
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        CreateGoalRequest req = new CreateGoalRequest();
        req.setTitle("Updated Title");
        req.setDesc("Updated Description");
        req.setCat(GoalCategory.BEHAVIORAL);
        req.setPri(GoalPriority.MEDIUM);
        req.setStartDt(LocalDate.now());
        req.setEndDt(LocalDate.now().plusMonths(2));
        req.setMgrId(2);

        Goal result = goalService.updateGoal(1, req, 1);

        assertEquals("Updated Title", result.getTitle());
        assertFalse(result.getRequestChanges(), "requestChanges should be reset to false");
        assertNotNull(result.getResubmittedDate());
    }

    @Test
    @DisplayName("updateGoal() should throw BadRequestException when changes not requested")
    void updateGoal_WithoutChangesRequested_ShouldThrowBadRequestException() {
        testGoal.setRequestChanges(false);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(BadRequestException.class,
                () -> goalService.updateGoal(1, new CreateGoalRequest(), 1));
    }

    @Test
    @DisplayName("updateGoal() should throw UnauthorizedException for wrong employee")
    void updateGoal_WithWrongEmployee_ShouldThrowUnauthorizedException() {
        testGoal.setRequestChanges(true);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(UnauthorizedException.class,
                () -> goalService.updateGoal(1, new CreateGoalRequest(), 999));
    }

    // ==================== deleteGoal() ====================

    @Test
    @DisplayName("deleteGoal() should soft-delete by changing status to REJECTED")
    void deleteGoal_ShouldSoftDelete() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        goalService.deleteGoal(1, 1, "EMPLOYEE");

        assertEquals(GoalStatus.REJECTED, testGoal.getStatus());
        verify(goalRepo).save(testGoal);
    }

    @Test
    @DisplayName("deleteGoal() should throw UnauthorizedException when employee deletes someone else's goal")
    void deleteGoal_ByWrongEmployee_ShouldThrowUnauthorizedException() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        assertThrows(UnauthorizedException.class,
                () -> goalService.deleteGoal(1, 999, "EMPLOYEE"));
    }

    @Test
    @DisplayName("deleteGoal() should allow manager/admin to delete any goal")
    void deleteGoal_ByManager_ShouldSucceed() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        // Manager can delete any goal (no ownership check for MANAGER role)
        assertDoesNotThrow(() -> goalService.deleteGoal(1, 2, "MANAGER"));
    }

    // ==================== verifyEvidence() ====================

    @Test
    @DisplayName("verifyEvidence() should update evidence verification status")
    void verifyEvidence_ShouldUpdateVerificationStatus() {
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        Goal result = goalService.verifyEvidence(1, 2, "VERIFIED", "Looks good");

        assertEquals(EvidenceVerificationStatus.VERIFIED, result.getEvidenceLinkVerificationStatus());
        assertEquals("Looks good", result.getEvidenceLinkVerificationNotes());
        assertNotNull(result.getEvidenceLinkVerifiedDate());
    }

    // ==================== rejectCompletion() ====================

    @Test
    @DisplayName("rejectCompletion() should set status back to IN_PROGRESS")
    void rejectCompletion_ShouldRevertToInProgress() {
        testGoal.setStatus(GoalStatus.PENDING_COMPLETION_APPROVAL);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(2)).thenReturn(Optional.of(manager));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);
        when(approvalRepo.save(any(GoalCompletionApproval.class))).thenReturn(new GoalCompletionApproval());

        Goal result = goalService.rejectCompletion(1, 2, "Not enough evidence");

        assertEquals(GoalStatus.IN_PROGRESS, result.getStatus());
        assertEquals(CompletionApprovalStatus.REJECTED, result.getCompletionApprovalStatus());
        assertEquals("Not enough evidence", result.getManagerCompletionComments());
    }

    // ==================== addProgressUpdate() ====================

    @Test
    @DisplayName("addProgressUpdate() should append new progress note")
    void addProgressUpdate_ShouldAppendNote() {
        testGoal.setProgressNotes(null); // No existing notes
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        goalService.addProgressUpdate(1, 1, "Started working on task 1");

        assertNotNull(testGoal.getProgressNotes());
        assertTrue(testGoal.getProgressNotes().contains("Started working on task 1"));
    }

    @Test
    @DisplayName("addProgressUpdate() should append to existing notes with newline")
    void addProgressUpdate_WithExistingNotes_ShouldAppend() {
        testGoal.setProgressNotes("2025-01-01: First update");
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));
        when(userRepo.findById(1)).thenReturn(Optional.of(employee));
        when(goalRepo.save(any(Goal.class))).thenReturn(testGoal);

        goalService.addProgressUpdate(1, 1, "Second update");

        assertTrue(testGoal.getProgressNotes().contains("First update"));
        assertTrue(testGoal.getProgressNotes().contains("Second update"));
        assertTrue(testGoal.getProgressNotes().contains("\n"), "Notes should be separated by newline");
    }

    // ==================== getProgressUpdates() ====================

    @Test
    @DisplayName("getProgressUpdates() should return progress notes")
    void getProgressUpdates_WithNotes_ShouldReturnNotes() {
        testGoal.setProgressNotes("Some progress notes here");
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        String result = goalService.getProgressUpdates(1);

        assertEquals("Some progress notes here", result);
    }

    @Test
    @DisplayName("getProgressUpdates() should return default message when no notes exist")
    void getProgressUpdates_WithNoNotes_ShouldReturnDefaultMessage() {
        testGoal.setProgressNotes(null);
        when(goalRepo.findById(1)).thenReturn(Optional.of(testGoal));

        String result = goalService.getProgressUpdates(1);

        assertEquals("No progress updates yet", result);
    }
}
