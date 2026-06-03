package com.ums.notification.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.notification.dto.CreateTemplateRequest;
import com.ums.notification.entity.NotificationTemplate;
import com.ums.notification.service.TemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService templateService;

	@PostMapping
	public ResponseEntity<NotificationTemplate> createTemplate(@RequestBody CreateTemplateRequest request) {

		log.info("Creating template: {}", request.getTemplateCode());

		NotificationTemplate template = templateService.createTemplate(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(template);
	}

	@GetMapping
	public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {

		return ResponseEntity.ok(templateService.getAllTemplates());
	}

	@GetMapping("/{templateCode}")
	public ResponseEntity<NotificationTemplate> getTemplateByCode(@PathVariable String templateCode) {

		return ResponseEntity.ok(templateService.getTemplateByCode(templateCode));
	}
}