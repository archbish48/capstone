package com.community.demo.repository;

import com.community.demo.domain.notice.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdAndReadFalse(Long userId);
}
