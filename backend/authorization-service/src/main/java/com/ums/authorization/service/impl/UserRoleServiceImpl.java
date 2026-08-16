package com.ums.authorization.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.authorization.entity.RoleRevocationOutbox;
import com.ums.authorization.entity.UserRole;
import com.ums.authorization.repository.RoleRevocationOutboxRepository;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.UserRoleService;
import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleServiceImpl implements UserRoleService {

	private final UserRoleRepository userRoleRepository;

        private final RoleRevocationOutboxRepository roleRevocationOutboxRepository;

        private final AuditPublisher auditPublisher;

	@Override
	public List<UserRole> getUserRoles(UUID userId) {

		return userRoleRepository.findByUserIdWithRole(userId);
	}

	@Override
	public List<UserRole> getActivePlatformUserRoles(UUID userId) {
		return userRoleRepository.findActivePlatformAssignments(userId);
	}

	@Override
	public List<UserRole> getActiveUserRoles(UUID userId, String scopeType, String scopeId) {
		if ("PLATFORM".equals(scopeType)) {
			return userRoleRepository.findActivePlatformAssignments(userId);
		}
		return userRoleRepository.findActiveAssignments(userId, scopeType, scopeId);
	}

	@Override
	public UserRole assignRole(UserRole userRole) {

		return userRoleRepository.save(userRole);
	}

	@Override
	@Transactional
	public UUID revokeRoleAssignment(UUID assignmentId, UUID revokedBy) {
		UserRole assignment = userRoleRepository.findByIdWithRole(assignmentId)
				.orElseThrow(() -> new com.ums.authorization.exception.RoleNotFoundException(
						"Role assignment not found"));

		if (!Boolean.TRUE.equals(assignment.getActive())) {
			return assignment.getUserId();
		}

		LocalDateTime revokedAt = LocalDateTime.now();
		RoleRevocationOutbox outbox = RoleRevocationOutbox.builder()
				.eventId(UUID.randomUUID())
				.assignmentId(assignment.getId())
				.userId(assignment.getUserId())
				.roleId(assignment.getRole().getId())
				.roleName(assignment.getRole().getName())
				.revokedBy(revokedBy)
				.revokedAt(revokedAt)
				.status(RoleRevocationOutbox.STATUS_PENDING)
				.build();

		assignment.setActive(false);
		userRoleRepository.save(assignment);
                roleRevocationOutboxRepository.save(outbox);

                publishAuditEvent(AuditEvent.builder()
                                .eventType("role.revoked")
                                .serviceName("authorization-service")
                                .userId(revokedBy == null ? null : revokedBy.toString())
                                .action("ROLE_REVOKE")
                                .entityType("ROLE_ASSIGNMENT")
                                .entityId(assignment.getId().toString())
                                .details("Revoked role " + assignment.getRole().getName()
                                                + " from user " + assignment.getUserId())
                                .timestamp(revokedAt)
                                .build());

		return assignment.getUserId();
	}
        private void publishAuditEvent(AuditEvent event) {
                try {
                        auditPublisher.publish(event);
                } catch (Exception ex) {
                        log.error(
                                        "Failed to publish RBAC audit event eventType={} entityId={}",
                                        event.getEventType(),
                                        event.getEntityId(),
                                        ex);
                }
        }
}