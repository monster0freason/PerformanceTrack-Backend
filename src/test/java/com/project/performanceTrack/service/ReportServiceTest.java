package com.project.performanceTrack.service;

import com.project.performanceTrack.entity.*;
import com.project.performanceTrack.enums.GoalStatus;
import com.project.performanceTrack.enums.PerformanceReviewStatus;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService.
 *
 * Tests report generation and dashboard analytics methods.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditLogRepository auditRepo;

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private PerformanceReviewRepository reviewRepo;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Report testReport;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John");
        testUser.setRole(UserRole.ADMIN);
        testUser.setDepartment("Engineering");

        testReport = new Report();
        testReport.setReportId(1);
        testReport.setScope("department");
        testReport.setMetrics("{\"goals\": 10}");
        testReport.setFormat("PDF");
        testReport.setGeneratedBy(testUser);
    }

    // ==================== getAllReports() ====================

    @Test
    @DisplayName("getAllReports() should return all reports")
    void getAllReports_ShouldReturnAll() {
        when(reportRepo.findAll()).thenReturn(Arrays.asList(testReport));

        List<Report> result = reportService.getAllReports();

        assertEquals(1, result.size());
    }

    // ==================== getReportById() ====================

    @Test
    @DisplayName("getReportById() should return report when found")
    void getReportById_WithValidId_ShouldReturn() {
        when(reportRepo.findById(1)).thenReturn(Optional.of(testReport));

        Report result = reportService.getReportById(1);

        assertNotNull(result);
        assertEquals("PDF", result.getFormat());
    }

    @Test
    @DisplayName("getReportById() should throw when not found")
    void getReportById_WithInvalidId_ShouldThrow() {
        when(reportRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reportService.getReportById(999));
    }

    // ==================== generateReport() ====================

    @Test
    @DisplayName("generateReport() should create and save a report")
    void generateReport_ShouldCreateReport() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(reportRepo.save(any(Report.class))).thenAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            saved.setReportId(10);
            return saved;
        });

        Report result = reportService.generateReport("department", "{}", "PDF", 1);

        assertNotNull(result);
        assertEquals("department", result.getScope());
        assertEquals("PDF", result.getFormat());
        assertNotNull(result.getFilePath());
        assertTrue(result.getFilePath().endsWith(".pdf"));

        // Verify audit log was created
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("generateReport() should throw when user not found")
    void generateReport_WithInvalidUser_ShouldThrow() {
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reportService.generateReport("scope", "{}", "CSV", 999));
    }

    // ==================== getDashboardMetrics() ====================

    @Test
    @DisplayName("getDashboardMetrics() for EMPLOYEE should return goal metrics")
    void getDashboardMetrics_AsEmployee_ShouldReturnGoalMetrics() {
        Goal completedGoal = new Goal();
        completedGoal.setStatus(GoalStatus.COMPLETED);
        Goal inProgressGoal = new Goal();
        inProgressGoal.setStatus(GoalStatus.IN_PROGRESS);
        Goal pendingGoal = new Goal();
        pendingGoal.setStatus(GoalStatus.PENDING);

        when(goalRepo.findByAssignedToUser_UserId(1))
                .thenReturn(Arrays.asList(completedGoal, inProgressGoal, pendingGoal));

        Map<String, Object> result = reportService.getDashboardMetrics(1, "EMPLOYEE");

        assertEquals(3, result.get("totalGoals"));
        assertEquals(1L, result.get("completedGoals"));
        assertEquals(1L, result.get("inProgressGoals"));
        assertEquals(1L, result.get("pendingGoals"));
    }

    @Test
    @DisplayName("getDashboardMetrics() for MANAGER should return team metrics")
    void getDashboardMetrics_AsManager_ShouldReturnTeamMetrics() {
        Goal pendingGoal = new Goal();
        pendingGoal.setStatus(GoalStatus.PENDING);

        when(goalRepo.findByAssignedManager_UserId(1)).thenReturn(Arrays.asList(pendingGoal));
        when(userRepo.findByManager_UserId(1)).thenReturn(Arrays.asList(testUser));

        Map<String, Object> result = reportService.getDashboardMetrics(1, "MANAGER");

        assertEquals(1, result.get("teamSize"));
        assertEquals(1, result.get("totalTeamGoals"));
        assertEquals(1L, result.get("pendingApprovals"));
    }

    @Test
    @DisplayName("getDashboardMetrics() for ADMIN should return system-wide metrics")
    void getDashboardMetrics_AsAdmin_ShouldReturnSystemMetrics() {
        when(userRepo.findAll()).thenReturn(Arrays.asList(testUser));
        when(goalRepo.findAll()).thenReturn(Collections.emptyList());
        when(reviewRepo.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = reportService.getDashboardMetrics(1, "ADMIN");

        assertEquals(1, result.get("totalUsers"));
        assertEquals(0, result.get("totalGoals"));
        assertEquals(0, result.get("totalReviews"));
    }

    // ==================== getPerformanceSummary() ====================

    @Test
    @DisplayName("getPerformanceSummary() should calculate average ratings")
    void getPerformanceSummary_ShouldCalculateAverages() {
        PerformanceReview review1 = new PerformanceReview();
        review1.setUser(testUser);
        review1.setEmployeeSelfRating(4);
        review1.setManagerRating(3);

        PerformanceReview review2 = new PerformanceReview();
        review2.setUser(testUser);
        review2.setEmployeeSelfRating(5);
        review2.setManagerRating(4);

        when(reviewRepo.findByCycle_CycleId(1)).thenReturn(Arrays.asList(review1, review2));

        Map<String, Object> result = reportService.getPerformanceSummary(1, null);

        assertEquals(2L, result.get("totalReviews"));
        assertEquals(4.5, result.get("avgSelfRating"));
        assertEquals(3.5, result.get("avgManagerRating"));
    }

    @Test
    @DisplayName("getPerformanceSummary() should filter by department")
    void getPerformanceSummary_WithDepartment_ShouldFilter() {
        PerformanceReview review1 = new PerformanceReview();
        review1.setUser(testUser); // Engineering dept
        review1.setEmployeeSelfRating(4);
        review1.setManagerRating(3);

        User hrUser = new User();
        hrUser.setDepartment("HR");
        PerformanceReview review2 = new PerformanceReview();
        review2.setUser(hrUser);
        review2.setEmployeeSelfRating(5);
        review2.setManagerRating(5);

        when(reviewRepo.findAll()).thenReturn(Arrays.asList(review1, review2));

        Map<String, Object> result = reportService.getPerformanceSummary(null, "Engineering");

        // Only review1 (Engineering) should be included
        assertEquals(1L, result.get("totalReviews"));
    }

    // ==================== getGoalAnalytics() ====================

    @Test
    @DisplayName("getGoalAnalytics() should return goal status breakdown")
    void getGoalAnalytics_ShouldReturnStatusBreakdown() {
        Goal goal1 = new Goal();
        goal1.setStatus(GoalStatus.COMPLETED);
        Goal goal2 = new Goal();
        goal2.setStatus(GoalStatus.IN_PROGRESS);
        Goal goal3 = new Goal();
        goal3.setStatus(GoalStatus.PENDING);

        when(goalRepo.findAll()).thenReturn(Arrays.asList(goal1, goal2, goal3));

        Map<String, Object> result = reportService.getGoalAnalytics();

        assertEquals(3, result.get("totalGoals"));
        assertEquals(1L, result.get("completed"));
        assertEquals(1L, result.get("inProgress"));
        assertEquals(1L, result.get("pending"));
    }

    // ==================== getDepartmentPerformance() ====================

    @Test
    @DisplayName("getDepartmentPerformance() should return metrics per department")
    void getDepartmentPerformance_ShouldReturnDeptMetrics() {
        User engUser = new User();
        engUser.setUserId(1);
        engUser.setDepartment("Engineering");

        Goal completedGoal = new Goal();
        completedGoal.setStatus(GoalStatus.COMPLETED);

        when(userRepo.findAll()).thenReturn(Arrays.asList(engUser));
        when(userRepo.findByDepartment("Engineering")).thenReturn(Arrays.asList(engUser));
        when(goalRepo.findByAssignedToUser_UserId(1)).thenReturn(Arrays.asList(completedGoal));

        List<Map<String, Object>> result = reportService.getDepartmentPerformance();

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).get("department"));
        assertEquals(1L, result.get(0).get("completedGoals"));
    }
}
