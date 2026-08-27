package com.ums.authorization.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.authorization.entity.Role;
import com.ums.authorization.entity.RoleRevocationOutbox;
import com.ums.authorization.entity.UserRole;
import com.ums.authorization.repository.RoleRevocationOutboxRepository;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTests {

	@Mock
	private UserRoleRepository userRoleRepository;

	@Mock
	private RoleRevocationOutboxRepository roleRevocationOutboxRepository;

	@Mock
	private AuditPublisher auditPublisher;

	@InjectMocks
	private UserRoleServiceImpl userRoleService;

	@Test
	void revokeRoleAssignmentDeactivatesRoleAndPersistsOutboxEvent() {
		UUID assignmentId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		UUID revokedBy = UUID.randomUUID();
		Role role = Role.builder().id(roleId).name("AUTH_ADMIN").build();
		UserRole assignment = UserRole.builder()
				.id(assignmentId)
				.userId(userId)
				.role(role)
				.scopeType("ORG")
				.scopeId("org-123")
				.active(true)
				.build();

		when(userRoleRepository.findByIdWithRole(assignmentId)).thenReturn(Optional.of(assignment));

		UUID result = userRoleService.revokeRoleAssignment(assignmentId, revokedBy);

		assertEquals(userId, result);
		assertFalse(assignment.getActive());
		verify(userRoleRepository).save(assignment);

		ArgumentCaptor<RoleRevocationOutbox> captor = ArgumentCaptor.forClass(RoleRevocationOutbox.class);
		verify(roleRevocationOutboxRepository).save(captor.capture());
		RoleRevocationOutbox outbox = captor.getValue();
		assertEquals(assignmentId, outbox.getAssignmentId());
		assertEquals(userId, outbox.getUserId());
		assertEquals(roleId, outbox.getRoleId());
		assertEquals("AUTH_ADMIN", outbox.getRoleName());
		assertEquals("ORG", outbox.getScopeType());
		assertEquals("org-123", outbox.getScopeId());
		assertEquals(revokedBy, outbox.getRevokedBy());
		assertEquals(RoleRevocationOutbox.STATUS_PENDING, outbox.getStatus());
	}

	@Test
	void revokeRoleAssignmentIsIdempotentWhenAlreadyInactive() {
		UUID assignmentId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID revokedBy = UUID.randomUUID();
		Role role = Role.builder().id(UUID.randomUUID()).name("EMPLOYEE").build();
		UserRole assignment = UserRole.builder()
				.id(assignmentId)
				.userId(userId)
				.role(role)
				.active(false)
				.build();

		when(userRoleRepository.findByIdWithRole(assignmentId)).thenReturn(Optional.of(assignment));

		UUID result = userRoleService.revokeRoleAssignment(assignmentId, revokedBy);

		assertEquals(userId, result);
		verify(userRoleRepository, never()).save(any(UserRole.class));
		verify(roleRevocationOutboxRepository, never()).save(any(RoleRevocationOutbox.class));
	}
}
