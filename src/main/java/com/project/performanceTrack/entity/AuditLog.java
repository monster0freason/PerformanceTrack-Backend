package com.project.performanceTrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * AuditLog Entity
 * ---------------
 * The permanent, immutable record of every significant event in the system.
 * Used by Admins to ensure compliance and monitor system health.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Integer auditId; // Unique sequence number for every logged event (e.g., AuditID: 9001)

    /**
     * The Actor (Workflow Phase: Phase 1, Step 1)
     * Identifies the user who performed the action.
     * Example: Admin (ID 500) logging in, or Rahul (ID 501) submitting a goal.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Integer relatedEntityId;

    @Column(name = "ip_address")
    private String ipAddress;

    private String status;

    private LocalDateTime timestamp;
}