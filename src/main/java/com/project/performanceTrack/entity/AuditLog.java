package com.project.performanceTrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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