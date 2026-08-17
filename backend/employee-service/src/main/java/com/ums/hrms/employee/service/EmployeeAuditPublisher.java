package com.ums.hrms.employee.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.employee.entity.Employee;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeAuditPublisher {

    private final AuditPublisher auditPublisher;

    public void publishCreated(Employee employee, UUID actorUserId) {
        publish("hrms.employee.created", "CREATE", employee, actorUserId, "Employee created successfully");
    }

    public void publishUpdated(Employee employee, UUID actorUserId) {
        publish("hrms.employee.updated", "UPDATE", employee, actorUserId, "Employee updated successfully");
    }

    private void publish(
            String eventType,
            String action,
            Employee employee,
            UUID actorUserId,
            String outcome) {
        try {
            auditPublisher.publish(AuditEvent.builder()
                    .eventType(eventType)
                    .serviceName("employee-service")
                    .userId(actorUserId.toString())
                    .action(action)
                    .entityType("EMPLOYEE")
                    .entityId(employee.getId().toString())
                    .details(outcome
                            + "; organizationId=" + employee.getOrganizationId()
                            + "; umsUserId=" + employee.getUmsUserId()
                            + "; employeeCode=" + employee.getEmployeeCode())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.error(
                    "Failed to publish HRMS employee audit event eventType={} employeeId={}",
                    eventType,
                    employee.getId(),
                    ex);
        }
    }
}
