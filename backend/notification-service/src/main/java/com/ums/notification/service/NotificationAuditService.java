package com.ums.notification.service;

public interface NotificationAuditService {

	void logSuccess(String eventType, String recipientEmail, String subject);

	void logFailure(String eventType, String recipientEmail, String subject, String errorMessage);
}