package com.project.performanceTrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer feedbackId;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private PerformanceReview review;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    private Goal goal;

    @ManyToOne
    @JoinColumn(name = "given_by_user_id", nullable = false)
    private User givenByUser;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "feedback_type", length = 50)
    private String feedbackType;

    @Column(nullable = false)
    private LocalDateTime date;
}