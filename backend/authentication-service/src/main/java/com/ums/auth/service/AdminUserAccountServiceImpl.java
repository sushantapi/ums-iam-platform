package com.ums.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.dto.admin.AdminUserAccountResponse;
import com.ums.auth.dto.admin.AdminUserMetricsResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.ResourceNotFoundException;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminUserAccountServiceImpl implements AdminUserAccountService {

	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final AuditPublisher auditPublisher;
	private final TokenBlacklistService tokenBlacklistService;

	@Value("${jwt.access-token-expiry-ms:900000}")
	private long accessTokenExpiryMs;

	@Override
	@Transactional(readOnly = true)
	public AdminUserAccountResponse getUser(UUID userId) {
		return toResponse(getRequiredUser(userId));
	}

	@Override
	@Transactional(readOnly = true)
	public AdminUserMetricsResponse getMetrics() {
		Instant now = Instant.now();
		return new AdminUserMetricsResponse(
				userRepository.count(),
				userRepository.countByStatus(UserStatus.ACTIVE),
				userRepository.countLockedUsers(now),
				userRepository.countByStatus(UserStatus.SUSPENDED));
	}

	@Override
	public void activateUser(UUID userId, UUID actorUserId) {
		User user = getRequiredUser(userId);
		if (user.getStatus() == UserStatus.DELETED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted users cannot be reactivated");
		}
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
		publishAudit("admin.user.activated", "USER_ACTIVATE", user, actorUserId);
	}

	@Override
	public void suspendUser(UUID userId, UUID actorUserId) {
		if (userId.equals(actorUserId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot suspend their own account");
		}

		User user = getRequiredUser(userId);
		if (user.getStatus() == UserStatus.DELETED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted users cannot be suspended");
		}

		user.setStatus(UserStatus.SUSPENDED);
		userRepository.save(user);

		var sessions = sessionRepository.findByUserId(userId);
		Instant now = Instant.now();
		long revocationTtlSeconds = Math.max(1L, accessTokenExpiryMs / 1000L);
		for (Session session : sessions) {
			if (!session.isRevoked()) {
				session.setRevoked(true);
				session.setRevokedAt(now);
			}
			tokenBlacklistService.revokeSession(session.getId(), revocationTtlSeconds);
		}
		if (!sessions.isEmpty()) {
			sessionRepository.saveAll(sessions);
		}

		publishAudit("admin.user.suspended", "USER_SUSPEND", user, actorUserId);
	}

	@Override
	public void unlockUser(UUID userId, UUID actorUserId) {
		User user = getRequiredUser(userId);
		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		userRepository.save(user);
		publishAudit("admin.user.unlocked", "USER_UNLOCK", user, actorUserId);
	}

	private User getRequiredUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}

	private AdminUserAccountResponse toResponse(User user) {
		return new AdminUserAccountResponse(
				user.getId(), user.getEmail(), user.getStatus().name(), user.isLocked(),
				user.getLockedUntil(), user.getLastLoginAt());
	}

	private void publishAudit(String eventType, String action, User user, UUID actorUserId) {
		try {
			auditPublisher.publish(AuditEvent.builder()
					.eventType(eventType)
					.serviceName("authentication-service")
					.userId(actorUserId.toString())
					.action(action)
					.entityType("USER")
					.entityId(user.getId().toString())
					.details(action + " completed for " + user.getEmail())
					.timestamp(LocalDateTime.now())
					.build());
		} catch (Exception ex) {
			log.warn("Audit delivery failed for {} on user {}", action, user.getId(), ex);
		}
	}
}
