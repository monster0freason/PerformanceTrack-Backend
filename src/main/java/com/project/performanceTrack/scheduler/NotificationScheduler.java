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

/*
 * This class is like an automatic reminder system for a performance tracking app.
 * Think of it as a virtual assistant that wakes up at specific times every day
 * to send reminder notifications to managers and employees.
 * It helps make sure nobody forgets about pending approvals or upcoming deadlines.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    /*
     * These are the tools we need to do our job:
     * - goalRepo: to find goals in the database
     * - cycleRepo: to check review cycles (like quarterly reviews)
     * - userRepo: to get the list of all users
     * - notificationService: to actually send the notifications
     */
    private final GoalRepository goalRepo;
    private final ReviewCycleRepository cycleRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    /*
     * This defines when we should send reminders about review cycles ending.
     * We remind people when there are 30, 15, 7, or 3 days left.
     * It's like countdown notifications - "Hey, only 7 days left!"
     */
    private static final Set<Long> REMINDER_DAYS = Set.of(30L, 15L, 7L, 3L);

    /*
     * TASK 1: Remind managers about goals that have been waiting for approval too long.
     *
     * The @Scheduled annotation makes this run automatically every day at 9 AM.
     * Think of it like setting an alarm clock that triggers this method.
     * The cron expression "0 0 9 * * *" means:
     * - 0 seconds, 0 minutes, 9 hours (9 AM), every day, every month, every day of week
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendPendingApprovalReminders() {

        log.info("Running: pending approval reminders");

        /*
         * Calculate what time it was 2 days ago.
         * We're looking for goals that have been sitting there since then.
         * It's like checking "What was the date 2 days ago?"
         */
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        /*
         * Find all goals that are:
         * 1. Still in PENDING status (waiting for approval)
         * 2. Were created more than 2 days ago
         * These are the "stale" goals that managers should act on.
         */
        List<Goal> staleGoals = goalRepo.findByStatusAndCreatedDateBefore(
                GoalStatus.PENDING, twoDaysAgo);

        /*
         * Now we organize these goals by manager.
         * Instead of sending one notification per goal (which could be annoying),
         * we count how many pending goals each manager has.
         * Result looks like: {Manager John -> 5 goals, Manager Sarah -> 3 goals}
         */
        Map<User, Long> countsByManager = staleGoals.stream()
                .collect(Collectors.groupingBy(
                        Goal::getAssignedManager, Collectors.counting()));

        /*
         * For each manager, send them ONE notification telling them
         * how many goals they need to review.
         * Much better than spamming them with individual notifications!
         */
        countsByManager.forEach((manager, count) -> {
            notificationService.sendNotification(
                    manager,
                    NotificationType.REVIEW_REMINDER,
                    "You have " + count + " goal(s) pending approval for over 2 days",
                    "Goal", null, "HIGH", true);
        });

        log.info("Completed: pending approval reminders. Notified {} managers", countsByManager.size());
    }

    /*
     * TASK 2: Remind employees when a review cycle is about to end.
     *
     * This runs every day at 10 AM (one hour after the manager reminders).
     * The idea is to give employees regular countdown reminders so they don't
     * miss the deadline for completing their performance goals.
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendReviewCycleEndingReminders() {

        log.info("Running: review cycle ending reminders");

        /*
         * First, find the current active review cycle.
         * A review cycle is like a quarter or semester - a time period where
         * employees need to complete their goals.
         * We only care about the most recent ACTIVE one.
         */
        cycleRepo.findFirstByStatusOrderByStartDateDesc(ReviewCycleStatus.ACTIVE)
                .ifPresent(cycle -> {

                    /*
                     * Calculate how many days are left until the cycle ends.
                     * It's like counting down: "Today is May 1st, cycle ends May 15th,
                     * so there are 14 days left."
                     */
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), cycle.getEndDate());

                    /*
                     * Check if we should send a reminder today.
                     * We only remind when there are exactly 30, 15, 7, or 3 days left.
                     * We don't want to spam people every single day!
                     */
                    if (REMINDER_DAYS.contains(daysLeft)) {

                        /*
                         * Get ALL users in the system.
                         * Everyone should be reminded about the upcoming deadline.
                         */
                        List<User> allUsers = userRepo.findAll();

                        /*
                         * Send a reminder to each user.
                         * The priority changes based on urgency:
                         * - 7 days or less? HIGH priority (red alert!)
                         * - More than 7 days? NORMAL priority (gentle heads-up)
                         */
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
                        /*
                         * Not a reminder day (like 20 days left or 5 days left).
                         * We skip sending notifications but log it anyway.
                         */
                        log.info("Completed: review cycle reminders. {} days left, no reminder needed", daysLeft);
                    }
                });
    }

    /*
     * TASK 3: Remind managers about completed goals waiting for their approval.
     *
     * This runs every Monday at 9 AM (notice the "MON" at the end of the cron).
     * When employees mark a goal as complete, managers need to review and approve it.
     * This reminds managers about completions that have been waiting 3+ days.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendPendingCompletionReminders() {

        log.info("Running: pending completion reminders");

        /*
         * Calculate what time it was 3 days ago.
         * We're looking for completions that have been waiting since then.
         */
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        /*
         * Find all goals where:
         * 1. Status is PENDING_COMPLETION_APPROVAL (employee said "I'm done!", waiting for manager)
         * 2. The completion was submitted more than 3 days ago
         * These managers are behind on their reviews!
         */
        List<Goal> staleCompletions = goalRepo.findByStatusAndCompletionSubmittedDateBefore(
                GoalStatus.PENDING_COMPLETION_APPROVAL, threeDaysAgo);

        /*
         * Group the stale completions by manager and count them.
         * Same strategy as Task 1 - one notification per manager instead of
         * one per goal.
         */
        Map<User, Long> countsByManager = staleCompletions.stream()
                .collect(Collectors.groupingBy(
                        Goal::getAssignedManager, Collectors.counting()));

        /*
         * Send each manager a HIGH priority notification.
         * Employees are waiting for feedback, so this is important!
         */
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
