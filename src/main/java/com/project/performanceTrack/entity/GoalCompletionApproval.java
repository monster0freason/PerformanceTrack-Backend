package com.project.performanceTrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "goal_completion_approvals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalCompletionApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Integer approvalId; // Unique ID for this specific approval transaction (Audit ID 6001)


    @ManyToOne
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "approval_decision", nullable = false, length = 40)
    private String approvalDecision;

    @ManyToOne
    @JoinColumn(name = "approved_by", nullable = false)
    private User approvedBy;

    @Column(name = "approval_date", nullable = false)
    private LocalDateTime approvalDate;

    @Column(name = "manager_comments", columnDefinition = "TEXT")
    private String managerComments;

    @Column(name = "evidence_link_verified")
    private Boolean evidenceLinkVerified;

    @Column(name = "decision_rationale", columnDefinition = "TEXT")
    private String decisionRationale;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
}