package com.ums.hrms.leave.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.leave.entity.LeaveRequest;
import com.ums.hrms.leave.entity.LeaveStatus;
import com.ums.hrms.leave.entity.LeaveType;

@ExtendWith(MockitoExtension.class)
class LeaveAuditPublisherTests {

    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private LeaveAuditPublisher leaveAuditPublisher;

    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void publishesCreatedEventUsingSharedAuditContract() {
        LeaveRequest leave = leave(LeaveStatus.PENDING);

        leaveAuditPublisher.publishCreated(leave, actorUserId);

        assertAudit(leave, LeaveAuditPublisher.LEAVE_CREATED, "CREATE", LeaveStatus.PENDING);
    }

    @Test
    void publishesApprovedEventUsingSharedAuditContract() {
        LeaveRequest leave = leave(LeaveStatus.APPROVED);

        leaveAuditPublisher.publishApproved(leave, actorUserId);

        assertAudit(leave, LeaveAuditPublisher.LEAVE_APPROVED, "APPROVE", LeaveStatus.APPROVED);
    }

    @Test
    void publishesRejectedEventUsingSharedAuditContract() {
        LeaveRequest leave = leave(LeaveStatus.REJECTED);

        leaveAuditPublisher.publishRejected(leave, actorUserId);

        assertAudit(leave, LeaveAuditPublisher.LEAVE_REJECTED, "REJECT", LeaveStatus.REJECTED);
    }

    @Test
    void publishesCancelledEventUsingSharedAuditContract() {
        LeaveRequest leave = leave(LeaveStatus.CANCELLED);

        leaveAuditPublisher.publishCancelled(leave, actorUserId);

        assertAudit(leave, LeaveAuditPublisher.LEAVE_CANCELLED, "CANCEL", LeaveStatus.CANCELLED);
    }

    @Test
    void publisherFailureDoesNotBreakLeaveCommand() {
        LeaveRequest leave = leave(LeaveStatus.PENDING);
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(auditPublisher)
                .publish(any(AuditEvent.class));

        assertDoesNotThrow(() -> leaveAuditPublisher.publishCreated(leave, actorUserId));
    }

    private void assertAudit(
            LeaveRequest leave,
            String expectedEventType,
            String expectedAction,
            LeaveStatus expectedStatus) {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPublisher).publish(captor.capture());

        AuditEvent event = captor.getValue();
        assertEquals(expectedEventType, event.getEventType());
        assertEquals("leave-service", event.getServiceName());
        assertEquals(actorUserId.toString(), event.getUserId());
        assertEquals(expectedAction, event.getAction());
        assertEquals("LEAVE_REQUEST", event.getEntityType());
        assertEquals(leave.getId().toString(), event.getEntityId());
        assertNotNull(event.getTimestamp());
        assertTrue(event.getDetails().contains("organizationId=" + leave.getOrganizationId()));
        assertTrue(event.getDetails().contains("employeeId=" + leave.getEmployeeId()));
        assertTrue(event.getDetails().contains("leaveType=" + leave.getLeaveType()));
        assertTrue(event.getDetails().contains("startDate=" + leave.getStartDate()));
        assertTrue(event.getDetails().contains("endDate=" + leave.getEndDate()));
        assertTrue(event.getDetails().contains("status=" + expectedStatus));
    }

    private LeaveRequest leave(LeaveStatus status) {
        LeaveRequest leave = new LeaveRequest();
        leave.setOrganizationId(UUID.randomUUID());
        leave.setEmployeeId(UUID.randomUUID());
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 11, 1));
        leave.setEndDate(LocalDate.of(2026, 11, 2));
        leave.setStatus(status);
        leave.setRequestedBy(actorUserId);
        return leave;
    }
}
