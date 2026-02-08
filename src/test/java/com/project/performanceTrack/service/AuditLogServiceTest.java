package com.project.performanceTrack.service;

import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService.
 *
 * Tests audit log retrieval (with various filters) and audit log creation.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditRepo;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;
    private AuditLog testLog;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John");
        testUser.setRole(UserRole.EMPLOYEE);

        testLog = new AuditLog();
        testLog.setAuditId(1);
        testLog.setUser(testUser);
        testLog.setAction("LOGIN");
        testLog.setDetails("User logged in");
        testLog.setStatus("SUCCESS");
        testLog.setTimestamp(LocalDateTime.now());
    }

    // ==================== getAuditLogs() (List version) ====================

    @Test
    @DisplayName("getAuditLogs() should filter by userId when provided")
    void getAuditLogs_ByUserId_ShouldFilterByUser() {
        when(auditRepo.findByUser_UserIdOrderByTimestampDesc(1)).thenReturn(Arrays.asList(testLog));

        List<AuditLog> result = auditLogService.getAuditLogs(1, null, null, null);

        assertEquals(1, result.size());
        assertEquals("LOGIN", result.get(0).getAction());
    }

    @Test
    @DisplayName("getAuditLogs() should filter by action when provided")
    void getAuditLogs_ByAction_ShouldFilterByAction() {
        when(auditRepo.findByActionOrderByTimestampDesc("LOGIN")).thenReturn(Arrays.asList(testLog));

        List<AuditLog> result = auditLogService.getAuditLogs(null, "LOGIN", null, null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getAuditLogs() should filter by date range when provided")
    void getAuditLogs_ByDateRange_ShouldFilterByDates() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        when(auditRepo.findByTimestampBetweenOrderByTimestampDesc(start, end))
                .thenReturn(Arrays.asList(testLog));

        List<AuditLog> result = auditLogService.getAuditLogs(null, null, start, end);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getAuditLogs() should return all when no filters provided")
    void getAuditLogs_NoFilters_ShouldReturnAll() {
        when(auditRepo.findAll()).thenReturn(Arrays.asList(testLog));

        List<AuditLog> result = auditLogService.getAuditLogs(null, null, null, null);

        assertEquals(1, result.size());
    }

    // ==================== getAuditLogs() (Paginated version) ====================

    @Test
    @DisplayName("getAuditLogs() paginated should filter by userId")
    void getAuditLogs_Paginated_ByUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Arrays.asList(testLog), pageable, 1);
        when(auditRepo.findByUser_UserId(1, pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(1, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getAuditLogs() paginated should filter by action")
    void getAuditLogs_Paginated_ByAction() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Arrays.asList(testLog), pageable, 1);
        when(auditRepo.findByAction("LOGIN", pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(null, "LOGIN", null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getAuditLogs() paginated should filter by date range")
    void getAuditLogs_Paginated_ByDateRange() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        Page<AuditLog> page = new PageImpl<>(Arrays.asList(testLog), pageable, 1);
        when(auditRepo.findByTimestampBetween(start, end, pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(null, null, start, end, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getAuditLogs() paginated should return all when no filters")
    void getAuditLogs_Paginated_NoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Arrays.asList(testLog), pageable, 1);
        when(auditRepo.findAll(pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    // ==================== logAudit() ====================

    @Test
    @DisplayName("logAudit() should save an audit log entry")
    void logAudit_ShouldSaveAuditLog() {
        // Act
        auditLogService.logAudit(testUser, "LOGIN", "User logged in", null, null, "SUCCESS");

        // Assert: verify the repo was called with any AuditLog object
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("logAudit() should default status to SUCCESS when null")
    void logAudit_WithNullStatus_ShouldDefaultToSuccess() {
        auditLogService.logAudit(testUser, "ACTION", "Details", "Entity", 1, null);

        // Verify save was called and capture the argument
        verify(auditRepo).save(argThat(log ->
                "SUCCESS".equals(log.getStatus())
        ));
    }

    // ==================== initiateExport() ====================

    @Test
    @DisplayName("initiateExport() should return a file path with correct format extension")
    void initiateExport_ShouldReturnPath() {
        String result = auditLogService.initiateExport("CSV");

        assertTrue(result.startsWith("/exports/audit_logs_"));
        assertTrue(result.endsWith(".csv"));
    }

    @Test
    @DisplayName("initiateExport() should handle PDF format")
    void initiateExport_WithPdf_ShouldReturnPdfPath() {
        String result = auditLogService.initiateExport("PDF");

        assertTrue(result.endsWith(".pdf"));
    }
}
