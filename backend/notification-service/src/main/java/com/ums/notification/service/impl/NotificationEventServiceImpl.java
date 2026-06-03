package com.ums.notification.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ums.notification.entity.NotificationEvent;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.repository.NotificationEventRepository;
import com.ums.notification.service.NotificationEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationEventServiceImpl implements NotificationEventService {

	private final NotificationEventRepository repository;

	@Override
	public NotificationEvent save(NotificationEvent event) {

		return repository.save(event);
	}

	@Override
	public List<NotificationEvent> getFailedEvents() {

		return repository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);
	}

	@Override
	public void markProcessed(Long eventId) {

		NotificationEvent event = repository.findById(eventId).orElseThrow();

		event.setStatus(NotificationStatus.SENT);

		event.setProcessedAt(LocalDateTime.now());
	}

	@Override
	public void markFailed(Long eventId, String error) {

		NotificationEvent event = repository.findById(eventId).orElseThrow();

		event.setStatus(NotificationStatus.FAILED);

		event.setRetryCount(event.getRetryCount() + 1);

		event.setErrorMessage(error);
	}
}