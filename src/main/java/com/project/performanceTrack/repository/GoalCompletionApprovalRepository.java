package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.GoalCompletionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Goal completion approval repository
@Repository
public interface GoalCompletionApprovalRepository extends JpaRepository<GoalCompletionApproval, Integer> {

    // Find approvals by goal ID
    List<GoalCompletionApproval> findByGoal_GoalId(Integer goalId);

    // Find approvals by approver
    List<GoalCompletionApproval> findByApprovedBy_UserId(Integer userId);
}