package com.ums.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.dto.AuditEventFilter;
import com.ums.dto.AuditEventPageResponse;
import com.ums.service.AuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/audit/events")
@RequiredArgsConstructor
public class InternalAuditController {

	private final AuditService auditService;

	@GetMapping
	public AuditEventPageResponse getEvents(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String actor,
			@RequestParam(required = false) String target, @RequestParam(required = false) String eventType,
			@RequestParam(required = false) String serviceName) {
		if (page < 0 || page > 100_000 || size < 1 || size > 200) {
			throw badRequest("Invalid page or size");
		}
		validateFilter(actor);
		validateFilter(target);
		validateFilter(eventType);
		validateFilter(serviceName);

		AuditEventFilter filter = new AuditEventFilter(actor, target, null, eventType, serviceName, null,
				(LocalDate) null, null);
		return AuditEventPageResponse.from(
				auditService.getEvents(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
	}

	private void validateFilter(String value) {
		if (value != null && value.length() > 255) {
			throw badRequest("Filter must not exceed 255 characters");
		}
	}

	private org.springframework.web.server.ResponseStatusException badRequest(String reason) {
		return new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.BAD_REQUEST, reason);
	}
}
