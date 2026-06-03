package com.ums.notification.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ums.notification.entity.NotificationEvent;
import com.ums.notification.service.NotificationEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

	private final NotificationEventService eventService;

	@Scheduled(fixedDelay = 300000)
	public void retryFailedNotifications() {

		log.info("Checking failed notifications");

		List<NotificationEvent> failedEvents = eventService.getFailedEvents();

		log.info("Found {} failed notifications", failedEvents.size());

		failedEvents.forEach(event -> {

			try {

				log.info("Retrying event {}", event.getId());

				// TODO: invoke strategy

				eventService.markProcessed(event.getId());

			} catch (Exception ex) {

				eventService.markFailed(event.getId(), ex.getMessage());
			}
		});
	}
}