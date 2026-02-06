package com.project.performanceTrack.service;

import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditRepo;

    // Filters and retrieves audit logs based on user ID, action type, or date range.
    // Supports various search criteria to help admins investigate system activities.
    // Returns a list of AuditLog entities ordered by the most recent timestamp.
    public List<AuditLog> getAuditLogs(Integer userId, String action, LocalDateTime startDt, LocalDateTime endDt) {
        if (userId != null) {
            return auditRepo.findByUser_UserIdOrderByTimestampDesc(userId);
        } else if (action != null) {
            return auditRepo.findByActionOrderByTimestampDesc(action);
        } else if (startDt != null && endDt != null) {
            return auditRepo.findByTimestampBetweenOrderByTimestampDesc(startDt, endDt);
        } else {
            return auditRepo.findAll();
        }
    }

    // Creates and persists a new audit record documenting a specific system event.
    // Captures details such as the actor, the action, the target entity, and the outcome.
    // Annotated with @Transactional to ensure the log is reliably saved.
    @Transactional
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

    // Prepares an export path for audit logs in a specified file format (e.g., CSV).
    // Generates a unique filename based on the current timestamp to avoid collisions.
    // Returns the string path where the generated export file will be located.
    public String initiateExport(String format) {
        return "/exports/audit_logs_" + System.currentTimeMillis() + "." + format.toLowerCase();
    }
}