package com.ums.notification.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.notification.entity.NotificationLog;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.repository.NotificationLogRepository;
import com.ums.notification.service.NotificationLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationLogServiceImpl implements NotificationLogService {

	private final NotificationLogRepository repository;

	@Override
	public List<NotificationLog> getAllLogs() {

		log.debug("Fetching all notification logs");

		return repository.findAll();
	}

	@Override
	public List<NotificationLog> getLogsByStatus(NotificationStatus status) {

		log.debug("Fetching notification logs by status: {}", status);

		return repository.findByStatus(status);
	}

	@Override
	public List<NotificationLog> getLogsByRecipient(String email) {

		log.debug("Fetching notification logs for recipient: {}", email);

		return repository.findByRecipientEmail(email);
	}
}