package com.ums.auth.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.auth.dto.admin.AdminSessionFilter;
import com.ums.auth.dto.admin.AdminSessionResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.repository.SessionRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSessionServiceImpl implements AdminSessionService {

	private final SessionRepository sessionRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<AdminSessionResponse> listSessions(AdminSessionFilter filter, Pageable pageable) {
		return sessionRepository.findAll(toSpecification(filter), pageable).map(this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminSessionResponse> listUserSessions(UUID userId) {
		return sessionRepository.findByUserId(userId).stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional
	public void revokeSession(UUID sessionId, UUID adminUserId) {
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
		revoke(session);
		sessionRepository.save(session);
	}

	@Override
	@Transactional
	public void revokeAllUserSessions(UUID userId, UUID adminUserId) {
		List<Session> sessions = sessionRepository.findByUserId(userId);
		sessions.forEach(this::revoke);
		sessionRepository.saveAll(sessions);
	}

	private Specification<Session> toSpecification(AdminSessionFilter filter) {
		return (root, query, builder) -> {
			Predicate predicate = builder.conjunction();

			if (filter.userId() != null) {
				predicate = builder.and(predicate, builder.equal(root.get("user").get("id"), filter.userId()));
			}
			if (filter.organizationId() != null) {
				predicate = builder.and(predicate,
						builder.equal(root.get("organizationId"), filter.organizationId()));
			}
			if (filter.from() != null) {
				predicate = builder.and(predicate,
						builder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay(filter.from())));
			}
			if (filter.to() != null) {
				predicate = builder.and(predicate,
						builder.lessThan(root.get("createdAt"), startOfDay(filter.to().plusDays(1))));
			}

			String status = filter.status() == null ? "" : filter.status().trim().toUpperCase(Locale.ROOT);
			Instant now = Instant.now();
			if ("ACTIVE".equals(status)) {
				predicate = builder.and(predicate, builder.isFalse(root.get("revoked")),
						builder.greaterThan(root.get("expiresAt"), now));
			} else if ("REVOKED".equals(status)) {
				predicate = builder.and(predicate, builder.isTrue(root.get("revoked")));
			} else if ("EXPIRED".equals(status)) {
				predicate = builder.and(predicate, builder.isFalse(root.get("revoked")),
						builder.lessThanOrEqualTo(root.get("expiresAt"), now));
			}

			return predicate;
		};
	}

	private Instant startOfDay(LocalDate date) {
		return date.atStartOfDay().toInstant(ZoneOffset.UTC);
	}

	private void revoke(Session session) {
		if (!session.isRevoked()) {
			session.setRevoked(true);
			session.setRevokedAt(Instant.now());
		}
	}

	private AdminSessionResponse toResponse(Session session) {
		String status = session.isRevoked()
				? "REVOKED"
				: session.getExpiresAt() != null && !session.getExpiresAt().isAfter(Instant.now())
						? "EXPIRED"
						: "ACTIVE";
		String userName = String.join(" ", session.getUser().getFirstName(), session.getUser().getLastName()).trim();

		return new AdminSessionResponse(
				session.getId(),
				session.getUser().getId(),
				userName,
				session.getOrganizationId(),
				null,
				session.getDeviceInfo(),
				session.getClient(),
				session.getIpAddress(),
				session.getCreatedAt(),
				session.getLastSeenAt(),
				session.getExpiresAt(),
				session.getRevokedAt(),
				status);
	}
}
