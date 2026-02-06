package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // Endpoint: GET /api/v1/audit-logs
    // Access: Strictly restricted to users with the 'ADMIN' role.
    // Supports optional query parameters for filtering logs by user, action, or date.
    @GetMapping
    public ApiResponse<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDt) {

        List<AuditLog> logs = auditLogService.getAuditLogs(userId, action, startDt, endDt);
        return ApiResponse.success("Audit logs retrieved", logs);
    }

    // Endpoint: POST /api/v1/audit-logs/export
    // Access: Restricted to 'ADMIN' role for data security.
    // Triggers the generation of an audit report in the requested file format.
    @PostMapping("/export")
    public ApiResponse<String> exportLogs(@RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "CSV");
        String filePath = auditLogService.initiateExport(format);
        return ApiResponse.success("Audit logs export initiated", filePath);
    }
}