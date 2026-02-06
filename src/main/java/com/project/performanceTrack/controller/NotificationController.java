package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.entity.Notification;
import com.project.performanceTrack.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications(HttpServletRequest httpReq,
                                                            @RequestParam(required = false) String status) {
        Integer userId = (Integer) httpReq.getAttribute("userId");
        List<Notification> notifications = notificationService.getNotifications(userId, status);
        return ApiResponse.success("Notifications retrieved", notifications);
    }

    @PutMapping("/{notifId}")
    public ApiResponse<Notification> markAsRead(@PathVariable Integer notifId) {
        Notification updated = notificationService.markAsRead(notifId);
        return ApiResponse.success("Notification marked as read", updated);
    }

    @PutMapping("/mark-all-read")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest httpReq) {
        Integer userId = (Integer) httpReq.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("All notifications marked as read");
    }
}