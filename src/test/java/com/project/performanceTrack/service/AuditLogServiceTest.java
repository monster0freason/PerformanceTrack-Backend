package com.project.performanceTrack.service;

/*
 * This file tests the AuditLogService class.
 *
 * AuditLogService is responsible for:
 * 1. Retrieving audit logs (with various filters like by user, by action, by date)
 * 2. Saving new audit log entries (recording what happened and when)
 * 3. Exporting logs to files (CSV, PDF)
 *
 * Think of audit logs like a security camera recording -
 * every important action is recorded with who did it, when, and what happened.
 */

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

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    /*
     * Fake repository - won't actually query the database.
     */
    @Mock
    private AuditLogRepository auditRepo;

    /*
     * Real service with the fake repository injected.
     */
    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;
    private AuditLog testLog;

    @BeforeEach
    void setUp() {

        /*
         * Set up a sample user who performed some action.
         */
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John");
        testUser.setRole(UserRole.EMPLOYEE);

        /*
         * Set up a sample audit log entry representing a login event.
         * This is the kind of record the system creates when someone logs in.
         */
        testLog = new AuditLog();
        testLog.setAuditId(1);
        testLog.setUser(testUser);
        testLog.setAction("LOGIN");
        testLog.setDetails("User logged in");
        testLog.setStatus("SUCCESS");
        testLog.setTimestamp(LocalDateTime.now());
    }

    /*
     * ==================== TESTS FOR getAuditLogs() - LIST VERSION ====================
     *
     * This version returns all matching logs as a simple list (no pagination).
     * We test each filter scenario separately - good practice to test one thing at a time!
     */

    @Test
    @DisplayName("getAuditLogs() should filter by userId when provided")
    void getAuditLogs_ByUserId_ShouldFilterByUser() {

        /*
         * ARRANGE: When the repo is asked for logs belonging to user ID 1,
         * return a list with our test log.
         */
        when(auditRepo.findByUser_UserIdOrderByTimestampDesc(1)).thenReturn(Arrays.asList(testLog));

        /*
         * ACT: Call with userId=1, everything else null (no other filters).
         */
        List<AuditLog> result = auditLogService.getAuditLogs(1, null, null, null);

        /*
         * ASSERT: We should get exactly 1 log, and it should be the LOGIN action.
         */
        assertEquals(1, result.size());
        assertEquals("LOGIN", result.get(0).getAction());
    }

    @Test
    @DisplayName("getAuditLogs() should filter by action when provided")
    void getAuditLogs_ByAction_ShouldFilterByAction() {

        /*
         * ARRANGE: When asked for all "LOGIN" action logs, return our test log.
         */
        when(auditRepo.findByActionOrderByTimestampDesc("LOGIN")).thenReturn(Arrays.asList(testLog));

        /*
         * ACT: Filter only by action, everything else null.
         */
        List<AuditLog> result = auditLogService.getAuditLogs(null, "LOGIN", null, null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getAuditLogs() should filter by date range when provided")
    void getAuditLogs_ByDateRange_ShouldFilterByDates() {

        /*
         * ARRANGE: Define a date range (all of 2025) and set up the mock
         * to return our test log when queried within that range.
         */
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        when(auditRepo.findByTimestampBetweenOrderByTimestampDesc(start, end))
                .thenReturn(Arrays.asList(testLog));

        /*
         * ACT: Filter only by date range, no user or action filter.
         */
        List<AuditLog> result = auditLogService.getAuditLogs(null, null, start, end);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getAuditLogs() should return all when no filters provided")
    void getAuditLogs_NoFilters_ShouldReturnAll() {

        /*
         * ARRANGE: When no filters are provided, the service should just get everything.
         */
        when(auditRepo.findAll()).thenReturn(Arrays.asList(testLog));

        /*
         * ACT: Call with all nulls - no filters at all.
         */
        List<AuditLog> result = auditLogService.getAuditLogs(null, null, null, null);

        assertEquals(1, result.size());
    }

    /*
     * ==================== TESTS FOR getAuditLogs() - PAGINATED VERSION ====================
     *
     * Pagination = getting results in pages (like page 1 of 10, page 2 of 10, etc.)
     * instead of loading ALL logs at once (which could be thousands!).
     *
     * These tests mirror the list version above but with pagination support.
     */

    @Test
    @DisplayName("getAuditLogs() paginated should filter by userId")
    void getAuditLogs_Paginated_ByUserId() {

        /*
         * PageRequest.of(0, 10) means: "Give me page 0 (first page), 10 items per page"
         * PageImpl is Spring's way of wrapping a list into a Page object.
         */
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Arrays.asList(testLog), pageable, 1);
        when(auditRepo.findByUser_UserId(1, pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(1, null, null, null, pageable);

        /*
         * getTotalElements() returns the total number of items across ALL pages.
         * Here we expect 1 total audit log.
         */
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

    /*
     * ==================== TESTS FOR logAudit() ====================
     */

    @Test
    @DisplayName("logAudit() should save an audit log entry")
    void logAudit_ShouldSaveAuditLog() {

        /*
         * ACT: Record a LOGIN action for our test user.
         */
        auditLogService.logAudit(testUser, "LOGIN", "User logged in", null, null, "SUCCESS");

        /*
         * ASSERT: Verify that the repository's save() method was actually called.
         * any(AuditLog.class) means "I don't care what the log object looks like,
         * just confirm save() was called with SOME AuditLog object".
         */
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("logAudit() should default status to SUCCESS when null")
    void logAudit_WithNullStatus_ShouldDefaultToSuccess() {

        /*
         * ACT: Pass null as the status - the service should default it to "SUCCESS".
         */
        auditLogService.logAudit(testUser, "ACTION", "Details", "Entity", 1, null);

        /*
         * ASSERT: Use argThat() to check the ACTUAL content of the saved object.
         * argThat() lets us write a custom condition: "Save was called with a log
         * whose status equals SUCCESS"
         *
         * This is more precise than any() - we're actually checking the data inside!
         */
        verify(auditRepo).save(argThat(log ->
                "SUCCESS".equals(log.getStatus())
        ));
    }

    /*
     * ==================== TESTS FOR initiateExport() ====================
     */

    @Test
    @DisplayName("initiateExport() should return a file path with correct format extension")
    void initiateExport_ShouldReturnPath() {

        /*
         * ACT: Request a CSV export.
         */
        String result = auditLogService.initiateExport("CSV");

        /*
         * ASSERT: Check that the returned file path:
         * 1. Starts with "/exports/audit_logs_" (correct folder and prefix)
         * 2. Ends with ".csv" (correct file extension)
         */
        assertTrue(result.startsWith("/exports/audit_logs_"));
        assertTrue(result.endsWith(".csv"));
    }

    @Test
    @DisplayName("initiateExport() should handle PDF format")
    void initiateExport_WithPdf_ShouldReturnPdfPath() {

        /*
         * Same test but for PDF format - makes sure the extension switches correctly.
         */
        String result = auditLogService.initiateExport("PDF");

        assertTrue(result.endsWith(".pdf"));
    }
}