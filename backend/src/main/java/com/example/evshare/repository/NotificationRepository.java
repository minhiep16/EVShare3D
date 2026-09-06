package com.example.evshare.repository;

import com.example.evshare.entity.Notification;
import com.example.evshare.entity.enums.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead);

    List<Notification> findByUserIdAndCategory(Long userId, NotificationCategory category);

    long countByUserIdAndIsReadFalse(Long userId);
}
