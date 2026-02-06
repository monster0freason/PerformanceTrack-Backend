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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Goal management service
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepo;

    private final UserRepository userRepo;

    private final AuditLogRepository auditRepo;

    private final FeedbackRepository fbRepo;

    private final GoalCompletionApprovalRepository approvalRepo;

    private final NotificationService notificationService;

    // Create new goal (Employee)
    @Transactional
    public Goal createGoal(CreateGoalRequest req, Integer empId) {
        // Get employee
        User emp = userRepo.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Get manager
        User mgr = userRepo.findById(req.getMgrId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        // Validate dates
        if (req.getEndDt().isBefore(req.getStartDt())) {
            throw new BadRequestException("End date must be after start date");
        }

        // Create goal
        Goal goal = new Goal();
        goal.setTitle(req.getTitle());
        goal.setDescription(req.getDesc());
        goal.setCategory(req.getCat());
        goal.setPriority(req.getPri());
        goal.setAssignedToUser(emp);
        goal.setAssignedManager(mgr);
        goal.setStartDate(req.getStartDt());
        goal.setEndDate(req.getEndDt());
        goal.setStatus(GoalStatus.PENDING);

        // Save goal
        Goal savedGoal = goalRepo.save(goal);

        // Create notification for manager
        notificationService.sendNotification(
                mgr,
                NotificationType.GOAL_SUBMITTED,
                emp.getName() + " submitted goal: " + goal.getTitle(),
                "Goal",
                savedGoal.getGoalId(),
                req.getPri().name(),
                true
        );

        // Create audit log
        createAuditLog(emp, "GOAL_CREATED", "Created goal: " + goal.getTitle(), "Goal", savedGoal.getGoalId());

        return savedGoal;
    }

    // Get goals by user
    public List<Goal> getGoalsByUser(Integer userId) {
        return goalRepo.findByAssignedToUser_UserId(userId);
    }

    // Get goals by manager
    public List<Goal> getGoalsByManager(Integer mgrId) {
        return goalRepo.findByAssignedManager_UserId(mgrId);
    }

    // Get goal by ID
    public Goal getGoalById(Integer goalId) {
        return goalRepo.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    // Approve goal (Manager)
    @Transactional
    public Goal approveGoal(Integer goalId, Integer mgrId) {
        Goal goal = getGoalById(goalId);

        // Check if manager is authorized
        if (!goal.getAssignedManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized to approve this goal");
        }

        // Check if goal is in pending status
        if (!goal.getStatus().equals(GoalStatus.PENDING)) {
            throw new BadRequestException("Goal is not in pending status");
        }

        // Update goal
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setApprovedBy(goal.getAssignedManager());
        goal.setApprovedDate(LocalDateTime.now());
        goal.setRequestChanges(false);
        Goal updated = goalRepo.save(goal);

        // Notify employee


        notificationService.sendNotification(
                goal.getAssignedToUser(),
                NotificationType.GOAL_APPROVED,
                "Your goal '" + goal.getTitle() + "' has been approved",
                "Goal",
                goalId,
                goal.getPriority().name(),
                false
        );

        // Audit log
        createAuditLog(goal.getAssignedManager(), "GOAL_APPROVED", "Approved goal: " + goal.getTitle(), "Goal", goalId);

        return updated;
    }

    // Request changes to goal (Manager)
    @Transactional
    public Goal requestChanges(Integer goalId, Integer mgrId, String comments) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Update goal
        goal.setRequestChanges(true);
        User mgr = userRepo.findById(mgrId).orElse(null);
        goal.setLastReviewedBy(mgr);
        goal.setLastReviewedDate(LocalDateTime.now());
        Goal updated = goalRepo.save(goal);

        // Save feedback
        Feedback fb = new Feedback();
        fb.setGoal(goal);
        fb.setGivenByUser(mgr);
        fb.setComments(comments);
        fb.setFeedbackType("CHANGE_REQUEST");
        fb.setDate(LocalDateTime.now());
        fbRepo.save(fb);

        // Notify employee
        notificationService.sendNotification(
                goal.getAssignedToUser(),
                NotificationType.GOAL_CHANGE_REQUESTED,
                "Changes requested for goal: " + goal.getTitle(),
                "Goal",
                goalId,
                "NORMAL",
                true
        );

        // Audit log
        createAuditLog(mgr, "GOAL_CHANGE_REQUESTED", "Requested changes for goal: " + goal.getTitle(), "Goal", goalId);

        return updated;
    }

    // Submit goal completion with evidence (Employee)
    @Transactional
    public Goal submitCompletion(Integer goalId, SubmitCompletionRequest req, Integer empId) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedToUser().getUserId().equals(empId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Check if goal is in progress
        if (!goal.getStatus().equals(GoalStatus.IN_PROGRESS)) {
            throw new BadRequestException("Goal is not in progress");
        }

        // Update goal with evidence and completion info
        goal.setStatus(GoalStatus.PENDING_COMPLETION_APPROVAL);
        goal.setEvidenceLink(req.getEvLink());
        goal.setEvidenceLinkDescription(req.getLinkDesc());
        goal.setEvidenceAccessInstructions(req.getAccessInstr());
        goal.setCompletionNotes(req.getCompNotes());
        goal.setCompletionSubmittedDate(LocalDateTime.now());
        goal.setCompletionApprovalStatus(CompletionApprovalStatus.PENDING);
        goal.setEvidenceLinkVerificationStatus(EvidenceVerificationStatus.NOT_VERIFIED);
        Goal updated = goalRepo.save(goal);

        // Notify manager
        notificationService.sendNotification(
                goal.getAssignedManager(),
                NotificationType.GOAL_COMPLETION_SUBMITTED,
                goal.getAssignedToUser().getName() + " submitted completion for: " + goal.getTitle(),
                "Goal",
                goalId,
                "HIGH",
                true
        );

        // Audit log
        createAuditLog(goal.getAssignedToUser(), "GOAL_COMPLETION_SUBMITTED", "Submitted completion", "Goal", goalId);

        return updated;
    }

    // Approve goal completion (Manager)
    public Goal approveCompletion(Integer goalId, ApproveCompletionRequest req, Integer mgrId) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Check status
        if (!goal.getStatus().equals(GoalStatus.PENDING_COMPLETION_APPROVAL)) {
            throw new BadRequestException("Goal is not pending completion approval");
        }

        // Update goal
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setCompletionApprovalStatus(CompletionApprovalStatus.APPROVED);
        User mgr = userRepo.findById(mgrId).orElse(null);
        goal.setCompletionApprovedBy(mgr);
        goal.setCompletionApprovedDate(LocalDateTime.now());
        goal.setFinalCompletionDate(LocalDateTime.now());
        goal.setManagerCompletionComments(req.getMgrComments());
        goal.setEvidenceLinkVerificationStatus(EvidenceVerificationStatus.VERIFIED);
        goal.setEvidenceLinkVerifiedBy(mgr);
        goal.setEvidenceLinkVerifiedDate(LocalDateTime.now());
        Goal updated = goalRepo.save(goal);

        // Create GoalCompletionApproval record
        GoalCompletionApproval approval = new GoalCompletionApproval();
        approval.setGoal(goal);
        approval.setApprovalDecision("APPROVED");
        approval.setApprovedBy(mgr);
        approval.setApprovalDate(LocalDateTime.now());
        approval.setManagerComments(req.getMgrComments());
        approval.setEvidenceLinkVerified(true);
        approval.setDecisionRationale("Evidence verified and goal completion approved");
        approvalRepo.save(approval);

        // Notify employee
        notificationService.sendNotification(
                goal.getAssignedToUser(),
                NotificationType.GOAL_COMPLETION_APPROVED,
                "Your goal '" + goal.getTitle() + "' completion has been approved!",
                "Goal",
                goalId,
                "HIGH",
                false // Action not required as it's a success notification
        );

        //Audit Log
        createAuditLog(mgr, "GOAL_COMPLETION_APPROVED", "Approved completion for goal: " + goal.getTitle(), "Goal", goalId);

        return updated;
    }

    // Request additional evidence (Manager)
    public Goal requestAdditionalEvidence(Integer goalId, Integer mgrId, String reason) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Update goal
        goal.setCompletionApprovalStatus(CompletionApprovalStatus.ADDITIONAL_EVIDENCE_REQUIRED);
        goal.setEvidenceLinkVerificationStatus(EvidenceVerificationStatus.NEEDS_ADDITIONAL_LINK);
        User mgr = userRepo.findById(mgrId).orElse(null);
        goal.setEvidenceLinkVerificationNotes(reason);
        Goal updated = goalRepo.save(goal);

        // Notify employee
        notificationService.sendNotification(
                goal.getAssignedToUser(),
                NotificationType.ADDITIONAL_EVIDENCE_REQUIRED,
                "Additional evidence needed for goal: " + goal.getTitle(),
                "Goal",
                goalId,
                "NORMAL",
                true
        );

        // Audit log
        createAuditLog(mgr, "ADDITIONAL_EVIDENCE_REQUESTED", "Requested additional evidence", "Goal", goalId);

        return updated;
    }
    // Helper method for Audit Logs to keep code clean
    private void createAuditLog(User user, String action, String details, String entityType, Integer entityId) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setDetails(details);
        log.setRelatedEntityType(entityType);
        log.setRelatedEntityId(entityId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);
    }

    // Update goal (Employee - only when changes requested)
    @Transactional
    public Goal updateGoal(Integer goalId, CreateGoalRequest req, Integer empId) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedToUser().getUserId().equals(empId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Check if changes were requested
        if (!goal.getRequestChanges()) {
            throw new BadRequestException("Goal is not in change request status");
        }

        // Update goal fields
        goal.setTitle(req.getTitle());
        goal.setDescription(req.getDesc());
        goal.setCategory(req.getCat());
        goal.setPriority(req.getPri());
        goal.setStartDate(req.getStartDt());
        goal.setEndDate(req.getEndDt());
        goal.setRequestChanges(false);
        goal.setResubmittedDate(LocalDateTime.now());

        Goal updated = goalRepo.save(goal);

        // Notify manager

        notificationService.sendNotification(
                goal.getAssignedManager(),
                NotificationType.GOAL_RESUBMITTED,
                goal.getAssignedToUser().getName() + " updated and resubmitted goal: " + goal.getTitle(),
                "Goal",
                goalId,
                "NORMAL",
                true
        );

        // Audit log
        createAuditLog(goal.getAssignedToUser(), "GOAL_UPDATED", "Updated and resubmitted goal: " + goal.getTitle(), "Goal", goalId);

        return updated;
    }

    // Delete goal (soft delete)
    @Transactional
    public void deleteGoal(Integer goalId, Integer userId, String role) {
        Goal goal = getGoalById(goalId);

        // Check authorization - employee can delete own goals, manager/admin can delete any
        if (role.equals("EMPLOYEE") && !goal.getAssignedToUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to delete this goal");
        }

        // Soft delete - just mark as rejected or inactive status
        goal.setStatus(GoalStatus.REJECTED);
        goalRepo.save(goal);

        // Audit log
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        createAuditLog(user, "GOAL_DELETED", "Deleted goal: " + goal.getTitle(), "Goal", goalId);

    }

    // Verify evidence (Manager)
    @Transactional
    public Goal verifyEvidence(Integer goalId, Integer mgrId, String status, String notes) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Update evidence verification status
        EvidenceVerificationStatus evStatus = EvidenceVerificationStatus.valueOf(status.toUpperCase());
        goal.setEvidenceLinkVerificationStatus(evStatus);
        goal.setEvidenceLinkVerificationNotes(notes);
        User mgr = userRepo.findById(mgrId).orElse(null);
        goal.setEvidenceLinkVerifiedBy(mgr);
        goal.setEvidenceLinkVerifiedDate(LocalDateTime.now());

        Goal updated = goalRepo.save(goal);

        // Audit log
        createAuditLog(mgr, "EVIDENCE_VERIFIED", "Verified evidence for goal: " + goal.getTitle() + " - Status: " + status, "Goal", goalId);

        return updated;
    }

    // Reject goal completion (Manager)
    @Transactional
    public Goal rejectCompletion(Integer goalId, Integer mgrId, String reason) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Update goal status back to in progress
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setCompletionApprovalStatus(CompletionApprovalStatus.REJECTED);
        goal.setManagerCompletionComments(reason);

        Goal updated = goalRepo.save(goal);

        // Create GoalCompletionApproval record for rejection
        GoalCompletionApproval approval = new GoalCompletionApproval();
        approval.setGoal(goal);
        approval.setApprovalDecision("REJECTED");
        User mgr = userRepo.findById(mgrId).orElse(null);
        approval.setApprovedBy(mgr);
        approval.setApprovalDate(LocalDateTime.now());
        approval.setManagerComments(reason);
        approval.setEvidenceLinkVerified(false);
        approval.setDecisionRationale("Goal completion rejected");
        approvalRepo.save(approval);

        // Notify employee
        notificationService.sendNotification(
                goal.getAssignedToUser(),
                NotificationType.GOAL_COMPLETION_APPROVED, // Note: You might want a specific REJECTED type if available
                "Your goal '" + goal.getTitle() + "' completion was rejected. Please review feedback.",
                "Goal",
                goalId,
                "HIGH",
                true
        );
        // Audit log
        createAuditLog(mgr, "GOAL_COMPLETION_REJECTED", "Rejected completion for goal: " + goal.getTitle(), "Goal", goalId);

        return updated;
    }

    // Add progress update (Employee)
    @Transactional
    public void addProgressUpdate(Integer goalId, Integer empId, String note) {
        Goal goal = getGoalById(goalId);

        // Check authorization
        if (!goal.getAssignedToUser().getUserId().equals(empId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Add progress note (append to existing notes with timestamp)
        String timestamp = LocalDateTime.now().toString();
        String newNote = timestamp + ": " + note;

        String existingNotes = goal.getProgressNotes();
        if (existingNotes == null || existingNotes.isEmpty()) {
            goal.setProgressNotes(newNote);
        } else {
            goal.setProgressNotes(existingNotes + "\n" + newNote);
        }

        goalRepo.save(goal);

        // Audit log
        User emp = userRepo.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        createAuditLog(emp, "PROGRESS_ADDED", "Added progress update for goal: " + goal.getTitle(), "Goal", goalId);
    }

    // Get progress updates
    public String getProgressUpdates(Integer goalId) {
        Goal goal = getGoalById(goalId);
        return goal.getProgressNotes() != null ? goal.getProgressNotes() : "No progress updates yet";
    }
}