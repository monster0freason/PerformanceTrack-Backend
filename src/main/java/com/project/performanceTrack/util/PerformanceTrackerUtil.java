package com.project.performanceTrack.util;
import com.project.performanceTrack.entity.*;
import com.project.performanceTrack.enums.*;
import com.project.performanceTrack.repository.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PerformanceTrackerUtil {

    private final NotificationRepository notifRepo;
    private final AuditLogRepository auditRepo;

    // Constructor injection (Spring will automatically inject these)
    public PerformanceTrackerUtil(NotificationRepository notifRepo, AuditLogRepository auditRepo) {
        this.notifRepo = notifRepo;
        this.auditRepo = auditRepo;
    }

    /**
     * Flexible Notification Creator
     */
    public void sendNotification(User user, NotificationType type, String message,
                                 String entityType, Integer entityId,
                                 String priority, boolean actionReq) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setMessage(message);
        notif.setRelatedEntityType(entityType);
        notif.setRelatedEntityId(entityId);
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setPriority(priority != null ? priority : "NORMAL");
        notif.setActionRequired(actionReq);
        notifRepo.save(notif);
    }

    /**
     * Flexible Audit Logger
     */
    public void logAudit(User user, String action, String details,
                         String entityType, Integer entityId, String status) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setDetails(details);
        log.setRelatedEntityType(entityType);
        log.setRelatedEntityId(entityId);
        log.setStatus(status != null ? status : "SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);
    }
}
