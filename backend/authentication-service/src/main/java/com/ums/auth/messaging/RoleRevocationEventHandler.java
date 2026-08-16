package com.ums.auth.messaging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.entity.ProcessedSecurityEvent;
import com.ums.auth.repository.ProcessedSecurityEventRepository;
import com.ums.auth.service.AdminSessionService;
import com.ums.events.event.role.RoleRevokedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleRevocationEventHandler {

	private static final String EVENT_TYPE = "role.revoked";

	private final ProcessedSecurityEventRepository processedSecurityEventRepository;
	private final AdminSessionService adminSessionService;

	@Transactional
	public boolean process(RoleRevokedEvent event) {
		validate(event);

		if (processedSecurityEventRepository.existsById(event.getEventId())) {
			return false;
		}

		adminSessionService.revokeAllUserSessions(event.getUserId(), event.getRevokedBy());
		processedSecurityEventRepository.save(ProcessedSecurityEvent.builder()
				.eventId(event.getEventId())
				.eventType(EVENT_TYPE)
				.userId(event.getUserId())
				.build());

		return true;
	}

	private void validate(RoleRevokedEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("RoleRevokedEvent is required");
		}
		if (event.getEventId() == null) {
			throw new IllegalArgumentException("RoleRevokedEvent.eventId is required");
		}
		if (event.getUserId() == null) {
			throw new IllegalArgumentException("RoleRevokedEvent.userId is required");
		}
	}
}
