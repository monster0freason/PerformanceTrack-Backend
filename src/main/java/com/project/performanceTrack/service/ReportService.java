package com.project.performanceTrack.service;

import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.entity.Report;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.entity.Goal;
import com.project.performanceTrack.entity.PerformanceReview;
import com.project.performanceTrack.enums.GoalStatus;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.ReportRepository;
import com.project.performanceTrack.repository.UserRepository;
import com.project.performanceTrack.repository.AuditLogRepository;
import com.project.performanceTrack.repository.GoalRepository;
import com.project.performanceTrack.repository.PerformanceReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

// Report service - handles report generation
@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuditLogRepository auditRepo;

    @Autowired
    private GoalRepository goalRepo;

    @Autowired
    private PerformanceReviewRepository reviewRepo;

    // Get all reports
    public List<Report> getAllReports() {
        return reportRepo.findAll();
    }

    // Get report by ID
    public Report getReportById(Integer reportId) {
        return reportRepo.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    // Get reports by user
    public List<Report> getReportsByUser(Integer userId) {
        return reportRepo.findByGeneratedBy_UserIdOrderByGeneratedDateDesc(userId);
    }

    // Generate report (Admin/Manager)
    public Report generateReport(String scope, String metrics, String format, Integer userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create report
        Report report = new Report();
        report.setScope(scope);
        report.setMetrics(metrics);
        report.setFormat(format);
        report.setGeneratedBy(user);
        report.setGeneratedDate(LocalDateTime.now());
        report.setFilePath("/reports/" + System.currentTimeMillis() + "." + format.toLowerCase());

        // Save report
        Report saved = reportRepo.save(report);

        // Create audit log
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction("REPORT_GENERATED");
        log.setDetails("Generated " + scope + " report in " + format + " format");
        log.setRelatedEntityType("Report");
        log.setRelatedEntityId(saved.getReportId());
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return saved;
    }

    // Get dashboard metrics
    public Map<String, Object> getDashboardMetrics(Integer userId, String role) {
        Map<String, Object> metrics = new HashMap<>();

        if (role.equals("EMPLOYEE")) {
            // Employee dashboard metrics
            List<Goal> myGoals = goalRepo.findByAssignedToUser_UserId(userId);
            long completedGoals = myGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
            long inProgressGoals = myGoals.stream().filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS).count();
            long pendingGoals = myGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING).count();

            metrics.put("totalGoals", myGoals.size());
            metrics.put("completedGoals", completedGoals);
            metrics.put("inProgressGoals", inProgressGoals);
            metrics.put("pendingGoals", pendingGoals);
            metrics.put("completionRate", myGoals.size() > 0 ? (completedGoals * 100.0 / myGoals.size()) : 0);

        } else if (role.equals("MANAGER")) {
            // Manager dashboard metrics
            List<Goal> teamGoals = goalRepo.findByAssignedManager_UserId(userId);
            List<User> teamMembers = userRepo.findByManager_UserId(userId);

            metrics.put("teamSize", teamMembers.size());
            metrics.put("totalTeamGoals", teamGoals.size());
            metrics.put("pendingApprovals", teamGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING).count());
            metrics.put("pendingCompletions", teamGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING_COMPLETION_APPROVAL).count());

        } else {
            // Admin dashboard metrics
            List<User> allUsers = userRepo.findAll();
            List<Goal> allGoals = goalRepo.findAll();
            List<PerformanceReview> allReviews = reviewRepo.findAll();

            metrics.put("totalUsers", allUsers.size());
            metrics.put("totalGoals", allGoals.size());
            metrics.put("totalReviews", allReviews.size());
            metrics.put("completedGoals", allGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count());
        }

        return metrics;
    }

    // Get performance summary
    public Map<String, Object> getPerformanceSummary(Integer cycleId, String dept) {
        Map<String, Object> summary = new HashMap<>();

        List<PerformanceReview> reviews;
        if (cycleId != null) {
            reviews = reviewRepo.findByCycle_CycleId(cycleId);
        } else {
            reviews = reviewRepo.findAll();
        }

        // Filter by department if provided
        if (dept != null && !dept.isEmpty()) {
            reviews = reviews.stream()
                    .filter(r -> dept.equals(r.getUser().getDepartment()))
                    .toList();
        }

        // Calculate metrics
        long totalReviews = reviews.size();
        double avgSelfRating = reviews.stream()
                .filter(r -> r.getEmployeeSelfRating() != null)
                .mapToInt(PerformanceReview::getEmployeeSelfRating)
                .average()
                .orElse(0.0);

        double avgManagerRating = reviews.stream()
                .filter(r -> r.getManagerRating() != null)
                .mapToInt(PerformanceReview::getManagerRating)
                .average()
                .orElse(0.0);

        summary.put("totalReviews", totalReviews);
        summary.put("avgSelfRating", avgSelfRating);
        summary.put("avgManagerRating", avgManagerRating);
        summary.put("cycleId", cycleId);
        summary.put("department", dept);

        return summary;
    }

    // Get goal analytics
    public Map<String, Object> getGoalAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        List<Goal> allGoals = goalRepo.findAll();

        // Status breakdown
        long pending = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING).count();
        long inProgress = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS).count();
        long pendingCompletion = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING_COMPLETION_APPROVAL).count();
        long completed = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
        long rejected = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.REJECTED).count();

        analytics.put("totalGoals", allGoals.size());
        analytics.put("pending", pending);
        analytics.put("inProgress", inProgress);
        analytics.put("pendingCompletion", pendingCompletion);
        analytics.put("completed", completed);
        analytics.put("rejected", rejected);
        analytics.put("completionRate", allGoals.size() > 0 ? (completed * 100.0 / allGoals.size()) : 0);

        return analytics;
    }

    // Get department performance
    public List<Map<String, Object>> getDepartmentPerformance() {
        List<Map<String, Object>> performance = new ArrayList<>();

        // Get all unique departments
        List<User> allUsers = userRepo.findAll();
        List<String> departments = allUsers.stream()
                .map(User::getDepartment)
                .filter(dept -> dept != null && !dept.isEmpty())
                .distinct()
                .toList();

        // For each department, calculate metrics
        for (String dept : departments) {
            Map<String, Object> deptMetrics = new HashMap<>();

            List<User> deptUsers = userRepo.findByDepartment(dept);
            List<Goal> deptGoals = new ArrayList<>();
            for (User user : deptUsers) {
                deptGoals.addAll(goalRepo.findByAssignedToUser_UserId(user.getUserId()));
            }

            long completedGoals = deptGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();

            deptMetrics.put("department", dept);
            deptMetrics.put("employeeCount", deptUsers.size());
            deptMetrics.put("totalGoals", deptGoals.size());
            deptMetrics.put("completedGoals", completedGoals);
            deptMetrics.put("completionRate", deptGoals.size() > 0 ? (completedGoals * 100.0 / deptGoals.size()) : 0);

            performance.add(deptMetrics);
        }

        return performance;
    }
}