package com.ums.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.dto.AuditEventFilter;
import com.ums.dto.AuditEventResponse;
import com.ums.entity.AuditLog;
import com.ums.repository.AuditLogRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

	private static final char LIKE_ESCAPE = '\\';

	private final AuditLogRepository repository;

	@Override
	@Transactional(readOnly = true)
	public Page<AuditEventResponse> getEvents(AuditEventFilter filter, Pageable pageable) {
		return repository.findAll(toSpecification(filter), pageable).map(this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public AuditEventResponse getEvent(long eventId) {
		return repository.findById(eventId)
				.map(this::toResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit event not found"));
	}

	private Specification<AuditLog> toSpecification(AuditEventFilter filter) {
		return (root, query, builder) -> {
			Predicate predicate = builder.conjunction();
			if (hasText(filter.actor())) {
				String value = contains(filter.actor());
				predicate = builder.and(predicate, builder.or(
						builder.like(builder.lower(root.get("userEmail")), value, LIKE_ESCAPE),
						builder.like(builder.lower(root.get("userId")), value, LIKE_ESCAPE)));
			}
			if (hasText(filter.target())) {
				String value = contains(filter.target());
				predicate = builder.and(predicate, builder.or(
						builder.like(builder.lower(root.get("userEmail")), value, LIKE_ESCAPE),
						builder.like(builder.lower(root.get("userId")), value, LIKE_ESCAPE),
						builder.like(builder.lower(root.get("entityId")), value, LIKE_ESCAPE)));
			}
			if (hasText(filter.eventType())) {
				predicate = builder.and(predicate,
						builder.like(builder.lower(root.get("eventType")), contains(filter.eventType()), LIKE_ESCAPE));
			}
			if (hasText(filter.serviceName())) {
				predicate = builder.and(predicate,
						builder.like(builder.lower(root.get("serviceName")), contains(filter.serviceName()), LIKE_ESCAPE));
			}
			if (hasText(filter.outcome())) {
				Predicate failed = builder.like(builder.lower(root.get("eventType")), "%fail%");
				predicate = builder.and(predicate,
						"FAILURE".equalsIgnoreCase(filter.outcome()) ? failed : builder.not(failed));
			}
			if (filter.from() != null) {
				predicate = builder.and(predicate,
						builder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay(filter.from())));
			}
			if (filter.to() != null) {
				predicate = builder.and(predicate,
						builder.lessThan(root.get("createdAt"), startOfDay(filter.to().plusDays(1))));
			}
			return predicate;
		};
	}

	private LocalDateTime startOfDay(LocalDate date) {
		return LocalDateTime.of(date, LocalTime.MIN);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String contains(String value) {
		String escaped = value.trim().toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}

	private AuditEventResponse toResponse(AuditLog event) {
		String outcome = event.getEventType() != null
				&& event.getEventType().toLowerCase(Locale.ROOT).contains("fail")
						? "FAILURE"
						: "SUCCESS";
		return new AuditEventResponse(
				event.getId(),
				String.valueOf(event.getId()),
				event.getEventType(),
				event.getAction(),
				event.getUserEmail() != null ? event.getUserEmail() : event.getUserId(),
				event.getEntityId(),
				event.getUserId(),
				event.getUserEmail(),
				event.getServiceName(),
				event.getEntityType(),
				event.getEntityId(),
				event.getIpAddress(),
				outcome,
				event.getDetails(),
				event.getCreatedAt(),
				event.getCreatedAt());
	}
}
