package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.CreateReviewCycleRequest;
import com.project.performanceTrack.entity.ReviewCycle;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.ReviewCycleStatus;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.ReviewCycleRepository;
import com.project.performanceTrack.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewCycleService {

    private final ReviewCycleRepository cycleRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService; // Updated

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
    @Transactional
    public ReviewCycle createCycle(CreateReviewCycleRequest req, Integer adminId){
        //create review cycle
        ReviewCycle cycle = new ReviewCycle();
        mapRequestToEntity(req, cycle);
        //save cycle
        ReviewCycle saved = cycleRepo.save(cycle);

        //create audit log
        User admin = userRepo.findById(adminId).orElse(null);

        auditLogService.logAudit(admin, "REVIEW_CYCLE_CREATED",
                "Created review cycle: " + cycle.getTitle(), "ReviewCycle", saved.getCycleId(), "SUCCESS");

        return saved;
    }

    //update review cycle (Admin)
    @Transactional
    public  ReviewCycle updateCycle(Integer cycleId, CreateReviewCycleRequest req, Integer adminId){
        ReviewCycle cycle = getCycleById(cycleId);
        mapRequestToEntity(req, cycle);
        //save cycle
        ReviewCycle updated = cycleRepo.save(cycle);
        User admin = userRepo.findById(adminId).orElse(null);
        auditLogService.logAudit(admin, "REVIEW_CYCLE_UPDATED",
                "Updated review cycle: " + cycle.getTitle(), "ReviewCycle", cycleId, "SUCCESS");
        return updated;
    }
    private void mapRequestToEntity(CreateReviewCycleRequest req, ReviewCycle cycle) {
        cycle.setTitle(req.getTitle());
        cycle.setStartDate(req.getStartDt());
        cycle.setEndDate(req.getEndDt());
        cycle.setStatus(req.getStatus());
    }

}