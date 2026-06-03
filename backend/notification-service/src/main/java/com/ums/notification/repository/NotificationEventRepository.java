package com.ums.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.notification.entity.NotificationEvent;
import com.ums.notification.enums.NotificationStatus;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

	List<NotificationEvent> findByStatus(NotificationStatus status);

	List<NotificationEvent> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer retryCount);
}