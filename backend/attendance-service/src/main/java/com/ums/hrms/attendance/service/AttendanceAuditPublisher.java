package com.ums.hrms.attendance.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.attendance.entity.AttendanceRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceAuditPublisher {

    private final AuditPublisher auditPublisher;

    public void publishCreated(AttendanceRecord attendance, UUID actorUserId) {
        publish(
                "hrms.attendance.created",
                "CREATE",
                attendance,
                actorUserId,
                "Attendance created successfully");
    }

    public void publishUpdated(AttendanceRecord attendance, UUID actorUserId) {
        publish(
                "hrms.attendance.updated",
                "UPDATE",
                attendance,
                actorUserId,
                "Attendance updated successfully");
    }

    private void publish(
            String eventType,
            String action,
            AttendanceRecord attendance,
            UUID actorUserId,
            String outcome) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .serviceName("attendance-service")
                .userId(actorUserId.toString())
                .action(action)
                .entityType("ATTENDANCE")
                .entityId(attendance.getId().toString())
                .details("%s; organizationId=%s; employeeId=%s; workDate=%s; status=%s".formatted(
                        outcome,
                        attendance.getOrganizationId(),
                        attendance.getEmployeeId(),
                        attendance.getWorkDate(),
                        attendance.getStatus()))
                .timestamp(LocalDateTime.now())
                .build();

        try {
            auditPublisher.publish(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to publish {} audit event for attendance {}",
                    eventType,
                    attendance.getId(),
                    exception);
        }
    }
}
