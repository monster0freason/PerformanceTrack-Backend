package com.project.performanceTrack.service;

import com.project.performanceTrack.entity.Notification;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.NotificationStatus;
import com.project.performanceTrack.enums.NotificationType;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class    NotificationService {

    private final NotificationRepository notifRepo;
    private final SseEmitterService sseEmitterService;

    public void sendNotification(User user, NotificationType type, String message,
                                 String entityType, Integer entityId,
                                 String priority, boolean actionReq) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setMessage(message);
        notif.setRelatedEntityType(entityType);
        notif.setRelatedEntityId(entityId);
        notif.setStatus(NotificationStatus.UNREAD);
        notif.setPriority(priority != null ? priority : "NORMAL");
        notif.setActionRequired(actionReq);
        Notification saved = notifRepo.save(notif);

        // Push to user in real-time if they're connected
        sseEmitterService.sendToUser(user.getUserId(), saved);
    }

    // Existing - keep (used by markAllAsRead)
    public List<Notification> getNotifications(Integer userId, String status) {
        if (status != null) {
            NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
            return notifRepo.findByUser_UserIdAndStatusOrderByCreatedDateDesc(userId, notifStatus);
        }
        return notifRepo.findByUser_UserIdOrderByCreatedDateDesc(userId);
    }

    // New - paginated
    public Page<Notification> getNotifications(Integer userId, String status, Pageable pageable) {
        if (status != null) {
            NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
            return notifRepo.findByUser_UserIdAndStatus(userId, notifStatus, pageable);
        }
        return notifRepo.findByUser_UserId(userId, pageable);
    }

    public Notification markAsRead(Integer notifId) {
        Notification notif = notifRepo.findById(notifId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notif.setStatus(NotificationStatus.READ);
        notif.setReadDate(LocalDateTime.now());
        return notifRepo.save(notif);
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unreadNotifs = notifRepo
                .findByUser_UserIdAndStatusOrderByCreatedDateDesc(userId, NotificationStatus.UNREAD);

        unreadNotifs.forEach(n -> {
            n.setStatus(NotificationStatus.READ);
            n.setReadDate(LocalDateTime.now());
        });

        notifRepo.saveAll(unreadNotifs);
    }
}