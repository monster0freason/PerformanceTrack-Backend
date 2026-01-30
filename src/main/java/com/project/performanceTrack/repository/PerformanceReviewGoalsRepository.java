

package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.PerformanceReviewGoals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PerformanceReviewGoalsRepository extends JpaRepository<PerformanceReviewGoals, Integer> {

    // Find links by review ID
    List<PerformanceReviewGoals> findByReview_ReviewId(Integer reviewId);

    // Find links by goal ID
    List<PerformanceReviewGoals> findByGoal_GoalId(Integer goalId);
}
