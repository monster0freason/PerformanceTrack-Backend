package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Audit log repository for database operations
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    // Find audit logs by user
    List<AuditLog> findByUser_UserIdOrderByTimestampDesc(Integer userId);

    // Find audit logs by action
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    // Find audit logs in date range
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
