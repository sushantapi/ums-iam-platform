package com.ums.notification.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ums.notification.entity.NotificationLog;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.repository.NotificationLogRepository;
import com.ums.notification.service.NotificationAuditService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ums.notification.entity.NotificationLog;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.repository.NotificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationAuditServiceImpl implements NotificationAuditService {

	private final NotificationLogRepository repository;

	@Override
	public void logSuccess(String eventType, String recipientEmail, String subject) {

		NotificationLog log = NotificationLog.builder().eventType(eventType).recipientEmail(recipientEmail)
				.recipient(recipientEmail).subject(subject).status(NotificationStatus.SENT).sentAt(LocalDateTime.now())
				.build();

		repository.save(log);
	}

	@Override
	public void logFailure(String eventType, String recipientEmail, String subject, String errorMessage) {

		NotificationLog log = NotificationLog.builder().eventType(eventType).recipientEmail(recipientEmail)
				.recipient(recipientEmail).subject(subject).status(NotificationStatus.FAILED).errorMessage(errorMessage)
				.build();

		repository.save(log);
	}
}