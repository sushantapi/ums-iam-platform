package com.ums.notification.consumer;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class RoleEventConsumerTests {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private RoleEventConsumer consumer;

	@Test
	void forwardsRoleAssignedEventsToNotificationService() {
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.userId(UUID.randomUUID())
				.roleId(UUID.randomUUID())
				.roleName("EMPLOYEE")
				.email("ada@example.com")
				.firstName("Ada")
				.assignedBy(UUID.randomUUID())
				.assignedAt(LocalDateTime.now())
				.build();

		consumer.consume(event);

		verify(notificationService).processRoleAssigned(event);
	}
}
