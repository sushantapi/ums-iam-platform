package com.ums.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.notification.entity.NotificationLog;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.service.NotificationLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationLogController {

	private final NotificationLogService notificationLogService;

	@GetMapping
	public ResponseEntity<List<NotificationLog>> getAllLogs() {

		return ResponseEntity.ok(notificationLogService.getAllLogs());
	}

	@GetMapping("/status/{status}")
	public ResponseEntity<List<NotificationLog>> getLogsByStatus(@PathVariable NotificationStatus status) {

		return ResponseEntity.ok(notificationLogService.getLogsByStatus(status));
	}

	@GetMapping("/recipient/{email}")
	public ResponseEntity<List<NotificationLog>> getLogsByRecipient(@PathVariable String email) {

		return ResponseEntity.ok(notificationLogService.getLogsByRecipient(email));
	}
}