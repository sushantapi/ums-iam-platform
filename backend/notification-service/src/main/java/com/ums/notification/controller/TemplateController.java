package com.ums.notification.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.notification.dto.CreateTemplateRequest;
import com.ums.notification.entity.NotificationTemplate;
import com.ums.notification.service.TemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService templateService;

	@PostMapping
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('NOTIFICATION_ADMIN') or hasAuthority('NOTIFICATION_TEMPLATE_WRITE')")
	public ResponseEntity<NotificationTemplate> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {

		log.info("Creating template: {}", request.getTemplateCode());

		NotificationTemplate template = templateService.createTemplate(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(template);
	}

	@GetMapping
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('NOTIFICATION_ADMIN') or hasAuthority('NOTIFICATION_TEMPLATE_READ')")
	public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {

		return ResponseEntity.ok(templateService.getAllTemplates());
	}

	@GetMapping("/{templateCode}")
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('NOTIFICATION_ADMIN') or hasAuthority('NOTIFICATION_TEMPLATE_READ')")
	public ResponseEntity<NotificationTemplate> getTemplateByCode(@PathVariable String templateCode) {

		return ResponseEntity.ok(templateService.getTemplateByCode(templateCode));
	}
}
