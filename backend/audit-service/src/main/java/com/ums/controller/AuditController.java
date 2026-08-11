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

	private static final int MAX_PAGE = 100_000;
	private static final int MAX_PAGE_SIZE = 200;
	private static final int MAX_FILTER_LENGTH = 255;

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
		validatePage(page, size);
		validateFilter("actor", actor);
		validateFilter("target", target);
		validateFilter("eventType", eventType);
		validateFilter("serviceName", serviceName);
		validateFilter("outcome", outcome);
		if (outcome != null && !outcome.isBlank()
				&& !"SUCCESS".equalsIgnoreCase(outcome)
				&& !"FAILURE".equalsIgnoreCase(outcome)) {
			throw badRequest("outcome must be SUCCESS or FAILURE");
		}
		if (organizationId != null && !organizationId.isBlank()) {
			throw badRequest("organizationId filtering is not supported by the current audit event schema");
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw badRequest("'from' must be on or before 'to'");
		}
		if (LocalDate.MAX.equals(to)) {
			throw badRequest("'to' is outside the supported date range");
		}

		AuditEventFilter filter = new AuditEventFilter(
				actor, target, organizationId, eventType, serviceName, outcome, from, to);
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return AuditEventPageResponse.from(auditService.getEvents(filter, pageable));
	}

	@GetMapping("/{eventId}")
	public AuditEventResponse getEvent(@PathVariable long eventId) {
		if (eventId <= 0) {
			throw badRequest("eventId must be positive");
		}
		return auditService.getEvent(eventId);
	}

	private void validatePage(int page, int size) {
		if (page < 0 || page > MAX_PAGE) {
			throw badRequest("page must be between 0 and " + MAX_PAGE);
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
		}
	}

	private void validateFilter(String name, String value) {
		if (value != null && value.length() > MAX_FILTER_LENGTH) {
			throw badRequest(name + " must not exceed " + MAX_FILTER_LENGTH + " characters");
		}
	}

	private org.springframework.web.server.ResponseStatusException badRequest(String reason) {
		return new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.BAD_REQUEST, reason);
	}
}
