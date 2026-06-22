package com.ums.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.dto.AuditEventFilter;
import com.ums.dto.AuditEventPageResponse;
import com.ums.dto.AuditEventResponse;
import com.ums.service.AuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit/events")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	@GetMapping
	public AuditEventPageResponse getEvents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String target,
			@RequestParam(required = false) String organizationId,
			@RequestParam(required = false) String eventType,
			@RequestParam(required = false) String serviceName,
			@RequestParam(required = false) String outcome,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		AuditEventFilter filter = new AuditEventFilter(
				actor, target, organizationId, eventType, serviceName, outcome, from, to);
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return AuditEventPageResponse.from(auditService.getEvents(filter, pageable));
	}

	@GetMapping("/{eventId}")
	public AuditEventResponse getEvent(@PathVariable long eventId) {
		return auditService.getEvent(eventId);
	}
}
