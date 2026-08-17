package com.ums.hrms.employee.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.employee.entity.Department;
import com.ums.hrms.employee.entity.Designation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationStructureAuditPublisher {

    private final AuditPublisher auditPublisher;

    public void publishDepartmentCreated(Department department, UUID actorUserId) {
        publish(
                "hrms.department.created",
                "CREATE",
                "DEPARTMENT",
                department.getId(),
                department.getOrganizationId(),
                department.getCode(),
                department.getName(),
                department.getStatus().name(),
                actorUserId,
                "Department created successfully");
    }

    public void publishDepartmentUpdated(Department department, UUID actorUserId) {
        publish(
                "hrms.department.updated",
                "UPDATE",
                "DEPARTMENT",
                department.getId(),
                department.getOrganizationId(),
                department.getCode(),
                department.getName(),
                department.getStatus().name(),
                actorUserId,
                "Department updated successfully");
    }

    public void publishDesignationCreated(Designation designation, UUID actorUserId) {
        publish(
                "hrms.designation.created",
                "CREATE",
                "DESIGNATION",
                designation.getId(),
                designation.getOrganizationId(),
                designation.getCode(),
                designation.getName(),
                designation.getStatus().name(),
                actorUserId,
                "Designation created successfully");
    }

    public void publishDesignationUpdated(Designation designation, UUID actorUserId) {
        publish(
                "hrms.designation.updated",
                "UPDATE",
                "DESIGNATION",
                designation.getId(),
                designation.getOrganizationId(),
                designation.getCode(),
                designation.getName(),
                designation.getStatus().name(),
                actorUserId,
                "Designation updated successfully");
    }

    private void publish(
            String eventType,
            String action,
            String entityType,
            UUID entityId,
            UUID organizationId,
            String code,
            String name,
            String status,
            UUID actorUserId,
            String outcome) {

        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .serviceName("employee-service")
                .userId(actorUserId.toString())
                .action(action)
                .entityType(entityType)
                .entityId(entityId.toString())
                .details("%s; organizationId=%s; code=%s; name=%s; status=%s".formatted(
                        outcome,
                        organizationId,
                        code,
                        name,
                        status))
                .timestamp(LocalDateTime.now())
                .build();

        try {
            auditPublisher.publish(event);
        } catch (Exception exception) {
            log.error("Failed to publish {} audit event for {} {}", eventType, entityType, entityId, exception);
        }
    }
}
