package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Audit log controller (Admin only)
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {


    private final AuditLogRepository auditRepo;

    // Get audit logs with filters
    @GetMapping
    public ApiResponse<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDt) {

        List<AuditLog> logs;

        if (userId != null) {
            logs = auditRepo.findByUser_UserIdOrderByTimestampDesc(userId);
        } else if (action != null) {
            logs = auditRepo.findByActionOrderByTimestampDesc(action);
        } else if (startDt != null && endDt != null) {
            logs = auditRepo.findByTimestampBetweenOrderByTimestampDesc(startDt, endDt);
        } else {
            logs = auditRepo.findAll();
        }

        return ApiResponse.success("Audit logs retrieved", logs);
    }

    // Export audit logs
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> exportLogs(@RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "CSV");
        String filePath = "/exports/audit_logs_" + System.currentTimeMillis() + "." + format.toLowerCase();
        // In real implementation, this would generate actual file
        return ApiResponse.success("Audit logs export initiated", filePath);
    }
}
