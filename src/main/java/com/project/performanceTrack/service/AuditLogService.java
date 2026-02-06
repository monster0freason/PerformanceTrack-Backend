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

    public String initiateExport(String format) {
        // Logic for actual file generation would go here
        return "/exports/audit_logs_" + System.currentTimeMillis() + "." + format.toLowerCase();
    }
}
