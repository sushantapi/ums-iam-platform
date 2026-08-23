package com.ums.org.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.ums.events.event.organization.OrganizationMfaRequiredEvent;
import com.ums.org.entity.OrganizationSecurityEventOutbox;
import com.ums.org.entity.OrganizationSecurityEventOutbox.Status;
import com.ums.org.repositoty.OrganizationSecurityEventOutboxRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationSecurityEventOutboxDispatcherTests {

	@Mock
	private OrganizationSecurityEventOutboxRepository outboxRepository;

	@Mock
	private OrganizationEventPublisher organizationEventPublisher;

	@InjectMocks
	private OrganizationSecurityEventOutboxDispatcher dispatcher;

	@Test
	void successfulDispatchPublishesAndMarksRowPublished() {
		OrganizationSecurityEventOutbox row = pendingRow();
		when(outboxRepository.findReadyForDispatch(eq(Status.PENDING), any(LocalDateTime.class), any(Pageable.class)))
				.thenReturn(List.of(row));

		dispatcher.dispatchPending();

		ArgumentCaptor<OrganizationMfaRequiredEvent> eventCaptor =
				ArgumentCaptor.forClass(OrganizationMfaRequiredEvent.class);
		verify(organizationEventPublisher).publishOrganizationMfaRequired(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventId()).isEqualTo(row.getEventId());
		assertThat(eventCaptor.getValue().getOrganizationId()).isEqualTo(row.getOrganizationId());
		assertThat(row.getStatus()).isEqualTo(Status.PUBLISHED);
		assertThat(row.getPublishedAt()).isNotNull();
		assertThat(row.getNextAttemptAt()).isNull();
		assertThat(row.getLastErrorType()).isNull();
		verify(outboxRepository).saveAll(List.of(row));
	}

	@Test
	void brokerFailureKeepsRowPendingAndSchedulesRetry() {
		OrganizationSecurityEventOutbox row = pendingRow();
		when(outboxRepository.findReadyForDispatch(eq(Status.PENDING), any(LocalDateTime.class), any(Pageable.class)))
				.thenReturn(List.of(row));
		doThrow(new IllegalStateException("broker unavailable"))
				.when(organizationEventPublisher)
				.publishOrganizationMfaRequired(any(OrganizationMfaRequiredEvent.class));

		dispatcher.dispatchPending();

		assertThat(row.getStatus()).isEqualTo(Status.PENDING);
		assertThat(row.getAttempts()).isEqualTo(1);
		assertThat(row.getNextAttemptAt()).isNotNull();
		assertThat(row.getPublishedAt()).isNull();
		assertThat(row.getLastErrorType()).isEqualTo(IllegalStateException.class.getName());
		verify(outboxRepository).saveAll(List.of(row));
	}

	private OrganizationSecurityEventOutbox pendingRow() {
		return OrganizationSecurityEventOutbox.builder()
				.eventId(UUID.randomUUID())
				.organizationId(UUID.randomUUID())
				.eventType("organization.security.mfa.required")
				.updatedBy(UUID.randomUUID())
				.occurredAt(LocalDateTime.now())
				.status(Status.PENDING)
				.build();
	}
}
