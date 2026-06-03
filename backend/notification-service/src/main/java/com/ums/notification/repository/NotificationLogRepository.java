package com.ums.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.notification.entity.NotificationLog;
import com.ums.notification.enums.NotificationStatus;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

	List<NotificationLog> findByRecipientEmail(String recipientEmail);

	List<NotificationLog> findByEventType(String eventType);

	List<NotificationLog> findByStatus(NotificationStatus status);
}