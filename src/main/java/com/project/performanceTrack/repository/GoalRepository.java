package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.Goal;
import com.project.performanceTrack.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Goal repository for database operations
@Repository
public interface GoalRepository extends JpaRepository<Goal, Integer> {

    // Find goals by assigned user
    List<Goal> findByAssignedToUser_UserId(Integer userId);

    // Find goals by manager
    List<Goal> findByAssignedManager_UserId(Integer managerId);

    // Find goals by status
    List<Goal> findByStatus(GoalStatus status);

    // Find goals by user and status
    List<Goal> findByAssignedToUser_UserIdAndStatus(Integer userId, GoalStatus status);

    // Find goals by manager and status
    List<Goal> findByAssignedManager_UserIdAndStatus(Integer managerId, GoalStatus status);
}