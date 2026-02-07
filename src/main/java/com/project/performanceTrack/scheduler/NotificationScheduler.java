package com.project.performanceTrack.scheduler;

import com.project.performanceTrack.entity.Goal;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.GoalStatus;
import com.project.performanceTrack.enums.NotificationType;
import com.project.performanceTrack.enums.ReviewCycleStatus;
import com.project.performanceTrack.repository.GoalRepository;
import com.project.performanceTrack.repository.ReviewCycleRepository;
import com.project.performanceTrack.repository.UserRepository;
import com.project.performanceTrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final GoalRepository goalRepo;
    private final ReviewCycleRepository cycleRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    private static final Set<Long> REMINDER_DAYS = Set.of(30L, 15L, 7L, 3L);

    // Task 1: Remind managers about goals sitting in PENDING for 2+ days
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    public void sendPendingApprovalReminders() {
        log.info("Running: pending approval reminders");

        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        List<Goal> staleGoals = goalRepo.findByStatusAndCreatedDateBefore(
                GoalStatus.PENDING, twoDaysAgo);

        // Group by manager: {manager -> count of stale goals}
        Map<User, Long> countsByManager = staleGoals.stream()
                .collect(Collectors.groupingBy(
                        Goal::getAssignedManager, Collectors.counting()));

        countsByManager.forEach((manager, count) -> {
            notificationService.sendNotification(
                    manager,
                    NotificationType.REVIEW_REMINDER,
                    "You have " + count + " goal(s) pending approval for over 2 days",
                    "Goal", null, "HIGH", true);
        });

        log.info("Completed: pending approval reminders. Notified {} managers", countsByManager.size());
    }

    // Task 2: Remind employees when review cycle is ending soon
    @Scheduled(cron = "0 0 10 * * *") // Daily at 10 AM
    public void sendReviewCycleEndingReminders() {
        log.info("Running: review cycle ending reminders");

        cycleRepo.findFirstByStatusOrderByStartDateDesc(ReviewCycleStatus.ACTIVE)
                .ifPresent(cycle -> {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), cycle.getEndDate());

                    if (REMINDER_DAYS.contains(daysLeft)) {
                        List<User> allUsers = userRepo.findAll();

                        allUsers.forEach(user -> {
                            notificationService.sendNotification(
                                    user,
                                    NotificationType.REVIEW_REMINDER,
                                    "Review cycle '" + cycle.getTitle() + "' ends in "
                                            + daysLeft + " days. Complete your goals.",
                                    "ReviewCycle", cycle.getCycleId(),
                                    daysLeft <= 7 ? "HIGH" : "NORMAL", false);
                        });

                        log.info("Completed: review cycle reminders. {} days left, notified {} users",
                                daysLeft, allUsers.size());
                    } else {
                        log.info("Completed: review cycle reminders. {} days left, no reminder needed", daysLeft);
                    }
                });
    }

    // Task 3: Remind managers about completions waiting for approval for 3+ days
    @Scheduled(cron = "0 0 9 * * MON") // Every Monday at 9 AM
    public void sendPendingCompletionReminders() {
        log.info("Running: pending completion reminders");

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        List<Goal> staleCompletions = goalRepo.findByStatusAndCompletionSubmittedDateBefore(
                GoalStatus.PENDING_COMPLETION_APPROVAL, threeDaysAgo);

        Map<User, Long> countsByManager = staleCompletions.stream()
                .collect(Collectors.groupingBy(
                        Goal::getAssignedManager, Collectors.counting()));

        countsByManager.forEach((manager, count) -> {
            notificationService.sendNotification(
                    manager,
                    NotificationType.REVIEW_REMINDER,
                    "You have " + count + " goal(s) pending completion approval for over 3 days",
                    "Goal", null, "HIGH", true);
        });

        log.info("Completed: pending completion reminders. Notified {} managers", countsByManager.size());
    }
}
