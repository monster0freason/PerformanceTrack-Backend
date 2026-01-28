package com.project.performanceTrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Performance review goals linking table - links reviews to goals
@Entity
@Table(name = "performance_review_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReviewGoals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Integer linkId;

    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    private PerformanceReview review;

    @ManyToOne
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "linked_date", nullable = false)
    private LocalDateTime linkedDate;
}
