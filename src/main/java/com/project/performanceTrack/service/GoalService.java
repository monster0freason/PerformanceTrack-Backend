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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Goal management service
@Service
public class GoalService {

    @Autowired
    private GoalRepository goalRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private NotificationRepository notifRepo;

    @Autowired
    private AuditLogRepository auditRepo;

    @Autowired
    private FeedbackRepository fbRepo;

    @Autowired
    private GoalCompletionApprovalRepository approvalRepo;

    // Create new goal (Employee)
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
        Notification notif = new Notification();
        notif.setUser(mgr);
        notif.setType(NotificationType.GOAL_SUBMITTED);
        notif.setMessage(emp.getName() + " submitted goal: " + goal.getTitle());
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(savedGoal.getGoalId());
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setPriority(req.getPri().name());
        notif.setActionRequired(true);
        notifRepo.save(notif);

        // Create audit log
        AuditLog log = new AuditLog();
        log.setUser(emp);
        log.setAction("GOAL_CREATED");
        log.setDetails("Created goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(savedGoal.getGoalId());
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedToUser());
        notif.setType(NotificationType.GOAL_APPROVED);
        notif.setMessage("Your goal '" + goal.getTitle() + "' has been approved");
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notifRepo.save(notif);

        // Audit log
        User mgr = userRepo.findById(mgrId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(mgr);
        log.setAction("GOAL_APPROVED");
        log.setDetails("Approved goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

    // Request changes to goal (Manager)
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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedToUser());
        notif.setType(NotificationType.GOAL_CHANGE_REQUESTED);
        notif.setMessage("Changes requested for goal: " + goal.getTitle());
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notifRepo.save(notif);

        // Audit log
        AuditLog log = new AuditLog();
        log.setUser(mgr);
        log.setAction("GOAL_CHANGE_REQUESTED");
        log.setDetails("Requested changes for goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

    // Submit goal completion with evidence (Employee)
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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedManager());
        notif.setType(NotificationType.GOAL_COMPLETION_SUBMITTED);
        notif.setMessage(goal.getAssignedToUser().getName() + " submitted completion for goal: " + goal.getTitle());
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setPriority("HIGH");
        notif.setActionRequired(true);
        notifRepo.save(notif);

        // Audit log
        User emp = userRepo.findById(empId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(emp);
        log.setAction("GOAL_COMPLETION_SUBMITTED");
        log.setDetails("Submitted completion for goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedToUser());
        notif.setType(NotificationType.GOAL_COMPLETION_APPROVED);
        notif.setMessage("Your goal '" + goal.getTitle() + "' completion has been approved!");
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setPriority("HIGH");
        notifRepo.save(notif);

        // Audit log
        AuditLog log = new AuditLog();
        log.setUser(mgr);
        log.setAction("GOAL_COMPLETION_APPROVED");
        log.setDetails("Approved completion for goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedToUser());
        notif.setType(NotificationType.ADDITIONAL_EVIDENCE_REQUIRED);
        notif.setMessage("Additional evidence needed for goal: " + goal.getTitle());
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setActionRequired(true);
        notifRepo.save(notif);

        // Audit log
        AuditLog log = new AuditLog();
        log.setUser(mgr);
        log.setAction("ADDITIONAL_EVIDENCE_REQUESTED");
        log.setDetails("Requested additional evidence for goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

    // Update goal (Employee - only when changes requested)
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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedManager());
        notif.setType(NotificationType.GOAL_RESUBMITTED);
        notif.setMessage(goal.getAssignedToUser().getName() + " updated and resubmitted goal: " + goal.getTitle());
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notifRepo.save(notif);

        // Audit log
        User emp = userRepo.findById(empId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(emp);
        log.setAction("GOAL_UPDATED");
        log.setDetails("Updated and resubmitted goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

    // Delete goal (soft delete)
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
        User user = userRepo.findById(userId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction("GOAL_DELETED");
        log.setDetails("Deleted goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);
    }

    // Verify evidence (Manager)
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
        AuditLog log = new AuditLog();
        log.setUser(mgr);
        log.setAction("EVIDENCE_VERIFIED");
        log.setDetails("Verified evidence for goal: " + goal.getTitle() + " - Status: " + status);
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

    // Reject goal completion (Manager)
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
        Notification notif = new Notification();
        notif.setUser(goal.getAssignedToUser());
        notif.setType(NotificationType.GOAL_COMPLETION_APPROVED);
        notif.setMessage("Your goal '" + goal.getTitle() + "' completion was rejected. Please review feedback.");
        notif.setRelatedEntityType("Goal");
        notif.setRelatedEntityId(goalId);
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setPriority("HIGH");
        notifRepo.save(notif);

        // Audit log
        AuditLog log = new AuditLog();
        log.setUser(mgr);
        log.setAction("GOAL_COMPLETION_REJECTED");
        log.setDetails("Rejected completion for goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

    // Add progress update (Employee)
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
        User emp = userRepo.findById(empId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(emp);
        log.setAction("PROGRESS_ADDED");
        log.setDetails("Added progress update for goal: " + goal.getTitle());
        log.setRelatedEntityType("Goal");
        log.setRelatedEntityId(goalId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);
    }

    // Get progress updates
    public String getProgressUpdates(Integer goalId) {
        Goal goal = getGoalById(goalId);
        return goal.getProgressNotes() != null ? goal.getProgressNotes() : "No progress updates yet";
    }
}