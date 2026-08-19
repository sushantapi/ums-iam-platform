package com.ums.hrms.leave.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.leave.entity.LeaveRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAuditPublisher {

    static final String LEAVE_CREATED = "hrms.leave.created";
    static final String LEAVE_APPROVED = "hrms.leave.approved";
    static final String LEAVE_REJECTED = "hrms.leave.rejected";
    static final String LEAVE_CANCELLED = "hrms.leave.cancelled";

    private final AuditPublisher auditPublisher;

    public void publishCreated(LeaveRequest leaveRequest, UUID actorUserId) {
        publish(
                LEAVE_CREATED,
                "CREATE",
                leaveRequest,
                actorUserId,
                "Leave request created successfully");
    }

    public void publishApproved(LeaveRequest leaveRequest, UUID actorUserId) {
        publish(
                LEAVE_APPROVED,
                "APPROVE",
                leaveRequest,
                actorUserId,
                "Leave request approved successfully");
    }

    public void publishRejected(LeaveRequest leaveRequest, UUID actorUserId) {
        publish(
                LEAVE_REJECTED,
                "REJECT",
                leaveRequest,
                actorUserId,
                "Leave request rejected successfully");
    }

    public void publishCancelled(LeaveRequest leaveRequest, UUID actorUserId) {
        publish(
                LEAVE_CANCELLED,
                "CANCEL",
                leaveRequest,
                actorUserId,
                "Leave request cancelled successfully");
    }

    private void publish(
            String eventType,
            String action,
            LeaveRequest leaveRequest,
            UUID actorUserId,
            String outcome) {
        try {
            auditPublisher.publish(AuditEvent.builder()
                    .eventType(eventType)
                    .serviceName("leave-service")
                    .userId(actorUserId.toString())
                    .action(action)
                    .entityType("LEAVE_REQUEST")
                    .entityId(leaveRequest.getId().toString())
                    .details("%s; organizationId=%s; employeeId=%s; leaveType=%s; startDate=%s; endDate=%s; status=%s"
                            .formatted(
                                    outcome,
                                    leaveRequest.getOrganizationId(),
                                    leaveRequest.getEmployeeId(),
                                    leaveRequest.getLeaveType(),
                                    leaveRequest.getStartDate(),
                                    leaveRequest.getEndDate(),
                                    leaveRequest.getStatus()))
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception exception) {
            log.error(
                    "Failed to publish HRMS leave audit event eventType={} leaveRequestId={}",
                    eventType,
                    leaveRequest.getId(),
                    exception);
        }
    }
}
