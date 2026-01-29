package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    // Find by goalId
    List<Feedback> findByGoal_GoalId(Integer goalId);

    // Find by reviewId
    List<Feedback> findByReview_ReviewId(Integer reviewId);
}