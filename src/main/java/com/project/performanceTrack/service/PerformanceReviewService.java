package com.project.performanceTrack.service;
import com.project.performanceTrack.dto.ManagerReviewRequest;
import com.project.performanceTrack.dto.SelfAssessmentRequest;
import com.project.performanceTrack.entity.*;
import com.project.performanceTrack.enums.GoalStatus;
import com.project.performanceTrack.enums.NotificationType;
import com.project.performanceTrack.enums.PerformanceReviewStatus;
import com.project.performanceTrack.exception.BadRequestException;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.exception.UnauthorizedException;
import com.project.performanceTrack.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceReviewService {

    private final PerformanceReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final ReviewCycleRepository cycleRepo;
    private final AuditLogRepository auditRepo;
    private final PerformanceReviewGoalsRepository reviewGoalsRepo;
    private final GoalRepository goalRepo;
    private final AuditLogService auditLogService; //updated
    private final NotificationService notificationService;

    //get reviews by user
    public List<PerformanceReview> getReviewsByUser(Integer userId){
        return reviewRepo.findByUser_UserId(userId);
    }

    //get reviews by cycle
    public List<PerformanceReview> getReviewsByCycle(Integer cycleId){
        return  reviewRepo.findByCycle_CycleId(cycleId);
    }

    // get review by ID
    public PerformanceReview getReviewById(Integer reviewId) {
        return reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    //submit self-assessment (Employee)
    @Transactional
    public PerformanceReview submitSelfAssessment(SelfAssessmentRequest req, Integer empId){
        //get employee
        User emp = userRepo.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        //get review cycle
        ReviewCycle cycle = cycleRepo.findById(req.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Review cycle not found"));

        //check if review already exists
        PerformanceReview review = reviewRepo
                .findByCycle_CycleIdAndUser_UserId(req.getCycleId(), empId)
                .orElse(null);

        if(review != null && review.getStatus() != PerformanceReviewStatus.PENDING){
            throw  new UnauthorizedException("Self-assessment already submitted");
        }

        // Create or update review
        if (review == null) {
            review = new PerformanceReview();
            review.setCycle(cycle);
            review.setUser(emp);
        }

        review.setSelfAssessment(req.getSelfAssmt());
        review.setEmployeeSelfRating(req.getSelfRating());
        review.setStatus(PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED);
        review.setSubmittedDate(LocalDateTime.now());

        //save review
        PerformanceReview saved = reviewRepo.save(review);

        //link completed goals to this review
        List<Goal> completedGoals = goalRepo.findByAssignedToUser_UserIdAndStatus(empId, GoalStatus.COMPLETED);
        for(Goal goal : completedGoals){
            PerformanceReviewGoals link = new PerformanceReviewGoals();
            link.setReview(saved);
            link.setGoal(goal);
            link.setLinkedDate(LocalDateTime.now());
            reviewGoalsRepo.save(link);
        }

        // Notifications using service class
        if (emp.getManager() != null) {
            notificationService.sendNotification(emp.getManager(), NotificationType.SELF_ASSESSMENT_SUBMITTED,
                    emp.getName() + " submitted self-assessment", "PerformanceReview", saved.getReviewId(), "HIGH", true);
        }


        // Centralized audit log
        auditLogService.logAudit(emp, "SELF_ASSESSMENT_SUBMITTED",
                "Submitted self-assessment for " + cycle.getTitle(), "PerformanceReview", saved.getReviewId(), "SUCCESS");
        return saved;

    }

    //update self-assessment draft (Employee)
    @Transactional
    public PerformanceReview updateSelfAssessmentDraft(Integer reviewId, SelfAssessmentRequest req, Integer empId){
        PerformanceReview review = getReviewById(reviewId);

        //check authorization
        if(!review.getUser().getUserId().equals(empId)){
            throw new UnauthorizedException("Not authorized");
        }

        // Can only update if still in pending or self-assessment status
        if (review.getStatus() != PerformanceReviewStatus.PENDING &&
                review.getStatus() != PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED) {
            throw new BadRequestException("Cannot update - review already completed");
        }

        //update self assessment
        review.setSelfAssessment((req.getSelfAssmt()));
        review.setEmployeeSelfRating(req.getSelfRating());

        //save without changing status
        PerformanceReview updated = reviewRepo.save(review);
        User emp = userRepo.findById(empId).orElse(null);
        //auditLog
        auditLogService.logAudit(emp, "SELF_ASSESSMENT_DRAFT_UPDATED",
                "Updated self-assessment draft", "PerformanceReview", reviewId, "SUCCESS");

        return updated;
    }

   // submit manager review (Manager)
    @Transactional
    public  PerformanceReview submitManagerReview(Integer reviewId, ManagerReviewRequest req, Integer mgrId){
        PerformanceReview review = getReviewById(reviewId);

        //check authorization
        if (!review.getUser().getManager().getUserId().equals(mgrId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Check if self-assessment is completed
        if (review.getStatus() != PerformanceReviewStatus.SELF_ASSESSMENT_COMPLETED) {
            throw new BadRequestException("Self-assessment not completed");
        }

        //update review
        User mgr = userRepo.findById(mgrId).orElse(null);
        review.setManagerFeedback(req.getMgrFb());
        review.setManagerRating(req.getMgrRating());
        review.setRatingJustification(req.getRatingJust());
        review.setCompensationRecommendations(req.getCompRec());
        review.setNextPeriodGoals(req.getNextGoals());
        review.setReviewedBy(mgr);
        review.setReviewCompletedDate(LocalDateTime.now());
        review.setStatus(PerformanceReviewStatus.COMPLETED);

        //save review
        PerformanceReview saved = reviewRepo.save(review);

        notificationService.sendNotification(review.getUser(), NotificationType.PERFORMANCE_REVIEW_COMPLETED,
                "Your performance review has been completed", "PerformanceReview", reviewId, "HIGH", false);

        auditLogService.logAudit(mgr, "MANAGER_REVIEW_COMPLETED",
                "Completed review for " + review.getUser().getName(), "PerformanceReview", reviewId, "SUCCESS");

        return saved;
    }

    //acknowledge review (Employee)
    @Transactional
    public PerformanceReview acknowledgeReview(Integer reviewId, Integer empId, String response){
        PerformanceReview review = getReviewById(reviewId);

        // Check authorization
        if (!review.getUser().getUserId().equals(empId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // Check if review is completed
        if (review.getStatus() != PerformanceReviewStatus.COMPLETED) {
            throw new BadRequestException("Review not completed");
        }

        //update review
        User emp = userRepo.findById(empId).orElse(null);
        review.setAcknowledgedBy(emp);
        review.setAcknowledgedDate(LocalDateTime.now());
        review.setEmployeeResponse(response);
        review.setStatus(PerformanceReviewStatus.COMPLETED_AND_ACKNOWLEDGED);

        //save review
        PerformanceReview saved = reviewRepo.save(review);

        // Use NotificationService
        if (review.getUser().getManager() != null) {
            notificationService.sendNotification(review.getUser().getManager(), NotificationType.REVIEW_ACKNOWLEDGED,
                    review.getUser().getName() + " acknowledged their review", "PerformanceReview", reviewId, "NORMAL", false);
        }
        //audit log
        auditLogService.logAudit(emp, "REVIEW_ACKNOWLEDGED",
                "Acknowledged performance review", "PerformanceReview", reviewId, "SUCCESS");

        return saved;

    }
}