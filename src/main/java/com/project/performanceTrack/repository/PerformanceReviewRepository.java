package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.PerformanceReview;
import com.project.performanceTrack.enums.PerformanceReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Integer> {

    //find reviews by user
    List<PerformanceReview> findByUser_UserId(Integer userId);

    //find reviews by cycle
    List<PerformanceReview> findByCycle_CycleId(Integer cycleId);

    //find reviews by status
    List<PerformanceReview> findByStatus(PerformanceReviewStatus status);


    //find reviews by cycle and user
    Optional<PerformanceReview> findByCycle_CycleIdAndUser_UserId(Integer userId, Integer cycleId);

}