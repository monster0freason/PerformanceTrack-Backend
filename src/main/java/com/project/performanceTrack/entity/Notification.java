package com.project.performanceTrack.entity;
import com.project.performanceTrack.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="notification_id")
    private Integer notificationId;

    @ManyToOne
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length = 50)
    private NotificationType type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name="related_entity_type",length=50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Integer relatedEntityId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(length = 20)
    private String priority;

    @Column(name = "action_required")
    private Boolean actionRequired = false;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "read_date")
    private LocalDateTime readDate;
}
}