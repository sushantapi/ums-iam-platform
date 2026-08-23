package com.ums.auth.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.entity.ProcessedSecurityEvent;
import com.ums.auth.entity.Session;
import com.ums.auth.repository.ProcessedSecurityEventRepository;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.service.TokenBlacklistService;
import com.ums.events.event.organization.OrganizationMfaRequiredEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationMfaRequiredEventHandler {

	private static final String EVENT_TYPE = "organization.security.mfa.required";

	private final ProcessedSecurityEventRepository processedSecurityEventRepository;
	private final SessionRepository sessionRepository;
	private final TokenBlacklistService blacklistService;

	@Transactional
	public boolean process(OrganizationMfaRequiredEvent event) {
		validate(event);

		if (processedSecurityEventRepository.existsById(event.getEventId())) {
			return false;
		}

		Instant now = Instant.now();
		List<Session> sessions =
				sessionRepository.findByOrganizationIdAndRevokedFalseAndMfaVerifiedFalseAndExpiresAtAfter(
						event.getOrganizationId(),
						now);

		for (Session session : sessions) {
			session.setRevoked(true);
			session.setRevokedAt(now);

			long ttlSeconds = Duration.between(now, session.getExpiresAt()).getSeconds();
			if (ttlSeconds > 0) {
				blacklistService.revokeSession(session.getId(), ttlSeconds);
			}
		}

		if (!sessions.isEmpty()) {
			sessionRepository.saveAll(sessions);
		}

		processedSecurityEventRepository.save(ProcessedSecurityEvent.builder()
				.eventId(event.getEventId())
				.eventType(EVENT_TYPE)
				.userId(event.getUpdatedBy())
				.build());

		return true;
	}

	private void validate(OrganizationMfaRequiredEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("OrganizationMfaRequiredEvent is required");
		}
		if (event.getEventId() == null) {
			throw new IllegalArgumentException("OrganizationMfaRequiredEvent.eventId is required");
		}
		if (event.getOrganizationId() == null) {
			throw new IllegalArgumentException("OrganizationMfaRequiredEvent.organizationId is required");
		}
		if (event.getUpdatedBy() == null) {
			throw new IllegalArgumentException("OrganizationMfaRequiredEvent.updatedBy is required");
		}
	}
}
