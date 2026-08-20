package com.ums.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.entity.AuditLog;
import com.ums.events.event.AuditEvent;
import com.ums.repository.AuditLogRepository;
import com.ums.service.AuditEventSanitizer;

@ExtendWith(MockitoExtension.class)
class AuditConsumerLeaveEventsTests {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private AuditEventSanitizer sanitizer;

    @InjectMocks
    private AuditConsumer auditConsumer;

    @ParameterizedTest
    @ValueSource(strings = {
            "hrms.leave.created",
            "hrms.leave.approved",
            "hrms.leave.rejected",
            "hrms.leave.cancelled"
    })
    void consumesLeaveLifecycleAuditEventsAndPersistsSanitizedLog(String eventType) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .serviceName("leave-service")
                .userId("9466dcac-b809-401a-9e46-a58b7c0dda82")
                .action("TEST")
                .entityType("LEAVE_REQUEST")
                .entityId("96976d16-20a9-4341-9fbf-72ffa958df2e")
                .details("leave audit consumer contract test")
                .timestamp(LocalDateTime.now())
                .build();

        AuditLog sanitized = AuditLog.builder()
                .eventType(eventType)
                .serviceName("leave-service")
                .entityType("LEAVE_REQUEST")
                .entityId(event.getEntityId())
                .createdAt(event.getTimestamp())
                .build();
        when(sanitizer.sanitize(event)).thenReturn(sanitized);

        auditConsumer.consume(event);

        verify(sanitizer).sanitize(event);
        verify(repository).save(sanitized);
        assertEquals(eventType, sanitized.getEventType());
    }
}
