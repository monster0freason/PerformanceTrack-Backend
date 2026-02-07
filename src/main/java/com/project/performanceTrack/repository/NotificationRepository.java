package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.Notification;
import com.project.performanceTrack.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {



    List<Notification> findByUser_UserIdOrderByCreatedDateDesc(Integer userId);
    List<Notification> findByUser_UserIdAndStatusOrderByCreatedDateDesc(Integer userId, NotificationStatus status);

    // New - paginated versions
    Page<Notification> findByUser_UserId(Integer userId, Pageable pageable);
    Page<Notification> findByUser_UserIdAndStatus(Integer userId, NotificationStatus status, Pageable pageable);
}
