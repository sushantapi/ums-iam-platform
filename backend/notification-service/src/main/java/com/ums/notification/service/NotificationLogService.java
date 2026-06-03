package com.ums.notification.service;

import java.util.List;

import com.ums.notification.entity.NotificationLog;
import com.ums.notification.enums.NotificationStatus;

public interface NotificationLogService {

	List<NotificationLog> getAllLogs();

	List<NotificationLog> getLogsByStatus(NotificationStatus status);

	List<NotificationLog> getLogsByRecipient(String email);
}