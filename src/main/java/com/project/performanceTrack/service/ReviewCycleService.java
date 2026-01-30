package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.CreateReviewCycleRequest;
import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.entity.ReviewCycle;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.ReviewCycleStatus;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.AuditLogRepository;
import com.project.performanceTrack.repository.ReviewCycleRepository;
import com.project.performanceTrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewCycleService {
    @Autowired
    private ReviewCycleRepository cycleRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private AuditLogRepository auditRepo;

    //get all review cycles
    public List<ReviewCycle> getAllCycles(){return cycleRepo.findAll();}

    //get cycle by id
    public ReviewCycle getCycleById(Integer cycleId){
        return cycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Review Cycle not found"));
    }

    //get active review cycle
    public ReviewCycle getActiveCycle(){
        return cycleRepo.findFirstByStatusOrderByStartDateDesc(ReviewCycleStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active review cycle found"));
    }

    //create review cycle (Admin)
    public ReviewCycle createCycle(CreateReviewCycleRequest req, Integer adminId){
        //create review cycle
        ReviewCycle cycle = new ReviewCycle();
        cycle.setTitle(req.getTitle());
        cycle.setStartDate(req.getStartDt());
        cycle.setEndDate(req.getEndDt());
        cycle.setStatus(req.getStatus());
        cycle.setRequiresCompletionApproval(req.getReqComAppr());
        cycle.setEvidenceRequired(req.getEvReq());

        //save cycle
        ReviewCycle saved = cycleRepo.save(cycle);

        //create audit log
        User admin = userRepo.findById(adminId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(admin);
        log.setAction("REVIEW_CYCLE_CREATED");
        log.setDetails("Created review cycle: " + cycle.getTitle());
        log.setRelatedEntityType("ReviewCycle");
        log.setRelatedEntityId(saved.getCycleId());
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return saved;
    }

    //update review cycle (Admin)

    public  ReviewCycle updateCycle(Integer cycleId, CreateReviewCycleRequest req, Integer adminId){
        ReviewCycle cycle = getCycleById(cycleId);

        //update the fields
        cycle.setTitle(req.getTitle());
        cycle.setStartDate(req.getStartDt());
        cycle.setEndDate(req.getEndDt());
        cycle.setStatus(req.getStatus());
        cycle.setRequiresCompletionApproval(req.getReqComAppr());
        cycle.setEvidenceRequired(req.getEvReq());

        //save cycle
        ReviewCycle updated = cycleRepo.save(cycle);

        // Create audit log
        User admin = userRepo.findById(adminId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(admin);
        log.setAction("REVIEW_CYCLE_UPDATED");
        log.setDetails("Updated review cycle: " + cycle.getTitle());
        log.setRelatedEntityType("ReviewCycle");
        log.setRelatedEntityId(cycleId);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }

}