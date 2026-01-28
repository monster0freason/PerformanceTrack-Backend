package com.project.performanceTrack.entity;

import com.project.performanceTrack.enums.ReviewCycleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Review cycle entity - represents a performance review period
@Entity
@Table(name = "review_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cycle_id")
    private Integer cycleId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewCycleStatus status;

    // Whether goal completion requires manager approval
    @Column(name = "requires_completion_approval", nullable = false)
    private Boolean requiresCompletionApproval = true;

    // Whether evidence is required for goals
    @Column(name = "evidence_required", nullable = false)
    private Boolean evidenceRequired = true;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
