package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.entity.Report;
import com.project.performanceTrack.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


// Report controller (Admin/Manager)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReportController {


    private final ReportService reportSvc;

    // Get all reports
    @GetMapping
    public ApiResponse<List<Report>> getAllReports() {
        List<Report> reports = reportSvc.getAllReports();
        return ApiResponse.success("Reports retrieved", reports);
    }

    // Get report by ID
    @GetMapping("/{reportId}")
    public ApiResponse<Report> getReportById(@PathVariable Integer reportId) {
        Report report = reportSvc.getReportById(reportId);
        return ApiResponse.success("Report retrieved", report);
    }

    // Generate report
    @PostMapping("/generate")
    public ApiResponse<Report> generateReport(@RequestBody Map<String, String> body,
                                              HttpServletRequest httpReq) {
        Integer userId = (Integer) httpReq.getAttribute("userId");
        String scope = body.get("scope");
        String metrics = body.get("metrics");
        String format = body.getOrDefault("format", "PDF");

        Report report = reportSvc.generateReport(scope, metrics, format, userId);
        return ApiResponse.success("Report generated", report);
    }

    // Get dashboard metrics
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard(HttpServletRequest httpReq) {
        String role = (String) httpReq.getAttribute("userRole");
        Integer userId = (Integer) httpReq.getAttribute("userId");
        Map<String, Object> metrics = reportSvc.getDashboardMetrics(userId, role);
        return ApiResponse.success("Dashboard metrics retrieved", metrics);
    }

    // Get performance summary
    @GetMapping("/performance-summary")
    public ApiResponse<Map<String, Object>> getPerformanceSummary(
            @RequestParam(required = false) Integer cycleId,
            @RequestParam(required = false) String dept) {
        Map<String, Object> summary = reportSvc.getPerformanceSummary(cycleId, dept);
        return ApiResponse.success("Performance summary retrieved", summary);
    }

    // Get goal analyticsv
    @GetMapping("/goal-analytics")
    public ApiResponse<Map<String, Object>> getGoalAnalytics() {
        Map<String, Object> analytics = reportSvc.getGoalAnalytics();
        return ApiResponse.success("Goal analytics retrieved", analytics);
    }

    // Get department performance
    @GetMapping("/department-performance")
    public ApiResponse<List<Map<String, Object>>> getDeptPerformance() {
        List<Map<String, Object>> performance = reportSvc.getDepartmentPerformance();
        return ApiResponse.success("Department performance retrieved", performance);
    }
}