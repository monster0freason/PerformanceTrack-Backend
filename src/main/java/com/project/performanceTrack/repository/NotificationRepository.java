package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.Notification;
import com.project.performanceTrack.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {


    List<Notification> findByUser_UserIdOrderByCreatedDateDesc(Integer userId);

    List<Notification> findByUser_UserIdAndStatusOrderByCreatedDateDesc(Integer userId, NotificationStatus status);
}