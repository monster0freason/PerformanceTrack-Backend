package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.entity.Notification;
import com.project.performanceTrack.enums.NotificationStatus;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.NotificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notifRepo;

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications(HttpServletRequest httpReq,
                                                            @RequestParam(required = false) String status) {
        Integer userId = (Integer) httpReq.getAttribute("userId");

        List<Notification> notifs;
        if (status != null) {
            NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
            notifs = notifRepo.findByUser_UserIdAndStatusOrderByCreatedDateDesc(userId, notifStatus);
        } else {
            notifs = notifRepo.findByUser_UserIdOrderByCreatedDateDesc(userId);
        }

        return ApiResponse.success("Notifications retrieved", notifs);
    }


    @PutMapping("/{notifId}")
    public ApiResponse<Notification> markAsRead(@PathVariable Integer notifId) {
        Notification notif = notifRepo.findById(notifId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notif.setStatus(NotificationStatus.READ);
        notif.setReadDate(LocalDateTime.now());
        Notification updated = notifRepo.save(notif);

        return ApiResponse.success("Notification marked as read", updated);
    }


    @PutMapping("/mark-all-read")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest httpReq) {
        Integer userId = (Integer) httpReq.getAttribute("userId");

        List<Notification> notifs = notifRepo
                .findByUser_UserIdAndStatusOrderByCreatedDateDesc(userId, NotificationStatus.UNREAD);

        notifs.forEach(n -> {
            n.setStatus(NotificationStatus.READ);
            n.setReadDate(LocalDateTime.now());
        });

        notifRepo.saveAll(notifs);

        return ApiResponse.success("All notifications marked as read");
    }
}