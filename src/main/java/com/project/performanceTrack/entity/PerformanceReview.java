package com.project.performanceTrack.entity;

import com.project.performanceTrack.enums.PerformanceReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Performance review entity
@Entity
@Table(name = "performance_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @ManyToOne
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Self-assessment stored as JSON string
    @Column(name = "self_assessment", columnDefinition = "TEXT")
    private String selfAssessment;

    @Column(name = "employee_self_rating")
    private Integer employeeSelfRating;

    // Manager feedback stored as JSON string
    @Column(name = "manager_feedback", columnDefinition = "TEXT")
    private String managerFeedback;

    @Column(name = "manager_rating")
    private Integer managerRating;

    @Column(name = "rating_justification", columnDefinition = "TEXT")
    private String ratingJustification;

    // Compensation recommendations stored as JSON string
    @Column(name = "compensation_recommendations", columnDefinition = "TEXT")
    private String compensationRecommendations;

    @Column(name = "next_period_goals", columnDefinition = "TEXT")
    private String nextPeriodGoals;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_completed_date")
    private LocalDateTime reviewCompletedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PerformanceReviewStatus status = PerformanceReviewStatus.PENDING;

    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @ManyToOne
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    @Column(name = "acknowledged_date")
    private LocalDateTime acknowledgedDate;

    @Column(name = "employee_response", columnDefinition = "TEXT")
    private String employeeResponse;

    @Column(name = "time_spent_minutes")
    private Integer timeSpentMinutes;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
