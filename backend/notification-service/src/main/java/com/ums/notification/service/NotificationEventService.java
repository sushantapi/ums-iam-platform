package com.ums.notification.service;

import java.util.List;

import com.ums.notification.entity.NotificationEvent;

public interface NotificationEventService {

	NotificationEvent save(NotificationEvent event);

	List<NotificationEvent> getFailedEvents();

	void markProcessed(Long eventId);

	void markFailed(Long eventId, String error);
}