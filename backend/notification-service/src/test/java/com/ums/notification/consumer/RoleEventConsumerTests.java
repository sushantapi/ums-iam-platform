package com.ums.notification.consumer;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.event.role.RoleRevokedEvent;
import com.ums.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class RoleEventConsumerTests {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private RoleEventConsumer consumer;

	@Test
	void assignedEventsAreDelegatedToNotificationService() {
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.eventId(UUID.randomUUID())
				.userId(UUID.randomUUID())
				.roleName("SUPER_ADMIN")
				.scopeType("PLATFORM")
				.scopeId("*")
				.build();

		consumer.consumeAssigned(event);

		verify(notificationService).processRoleAssigned(event);
	}

	@Test
	void revokedEventsAreDelegatedToNotificationService() {
		RoleRevokedEvent event = RoleRevokedEvent.builder()
				.eventId(UUID.randomUUID())
				.userId(UUID.randomUUID())
				.roleName("SUPER_ADMIN")
				.scopeType("PLATFORM")
				.scopeId("*")
				.build();

		consumer.consumeRevoked(event);

		verify(notificationService).processRoleRevoked(event);
	}
}
