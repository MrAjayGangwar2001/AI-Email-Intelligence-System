package com.emailintelligence.repository;

import com.emailintelligence.entity.EmailNotification;
import com.emailintelligence.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, UUID> {
    List<EmailNotification> findByStatus(NotificationStatus status);
}
