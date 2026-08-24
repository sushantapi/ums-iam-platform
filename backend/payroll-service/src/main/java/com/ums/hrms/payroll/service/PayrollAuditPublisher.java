package com.ums.hrms.payroll.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.StatutoryPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollAuditPublisher {

    static final String SALARY_STRUCTURE_CREATED = "hrms.payroll.salary-structure.created";
    static final String SALARY_STRUCTURE_SUPERSEDED = "hrms.payroll.salary-structure.superseded";
    static final String STATUTORY_POLICY_CREATED = "hrms.payroll.statutory-policy.created";
    static final String PAYROLL_RUN_PROCESSED = "hrms.payroll.run.processed";
    static final String PAYROLL_RUN_FINALIZED = "hrms.payroll.run.finalized";

    private final AuditPublisher auditPublisher;

    public void publishSalaryStructureCreated(SalaryStructure structure, UUID actorUserId) {
        AuditEvent event = baseEvent(
                SALARY_STRUCTURE_CREATED,
                "CREATE",
                "SALARY_STRUCTURE",
                structure.getId(),
                actorUserId,
                "Salary structure created; organizationId=%s; employeeId=%s; version=%d; currency=%s; effectiveFrom=%s; effectiveTo=%s; active=%s"
                        .formatted(
                                structure.getOrganizationId(),
                                structure.getEmployeeId(),
                                structure.getVersionNumber(),
                                structure.getCurrency(),
                                structure.getEffectiveFrom(),
                                structure.getEffectiveTo(),
                                structure.isActive()));
        publishAfterCommit(event);
    }

    public void publishSalaryStructureSuperseded(
            SalaryStructure predecessor,
            SalaryStructure successor,
            UUID actorUserId) {
        AuditEvent event = baseEvent(
                SALARY_STRUCTURE_SUPERSEDED,
                "SUPERSEDE",
                "SALARY_STRUCTURE",
                successor.getId(),
                actorUserId,
                "Salary structure superseded; organizationId=%s; employeeId=%s; predecessorId=%s; predecessorVersion=%d; successorId=%s; successorVersion=%d; predecessorEffectiveTo=%s; successorEffectiveFrom=%s"
                        .formatted(
                                successor.getOrganizationId(),
                                successor.getEmployeeId(),
                                predecessor.getId(),
                                predecessor.getVersionNumber(),
                                successor.getId(),
                                successor.getVersionNumber(),
                                predecessor.getEffectiveTo(),
                                successor.getEffectiveFrom()));
        publishAfterCommit(event);
    }

    public void publishStatutoryPolicyCreated(StatutoryPolicy policy, UUID actorUserId) {
        AuditEvent event = baseEvent(
                STATUTORY_POLICY_CREATED,
                "CREATE",
                "STATUTORY_POLICY",
                policy.getId(),
                actorUserId,
                "Statutory policy created; organizationId=%s; countryCode=%s; policyVersion=%s; effectiveFrom=%s; effectiveTo=%s; active=%s"
                        .formatted(
                                policy.getOrganizationId(),
                                policy.getCountryCode(),
                                policy.getPolicyVersion(),
                                policy.getEffectiveFrom(),
                                policy.getEffectiveTo(),
                                policy.isActive()));
        publishAfterCommit(event);
    }

    public void publishPayrollRunProcessed(PayrollRun run, UUID actorUserId, int entryCount) {
        AuditEvent event = baseEvent(
                PAYROLL_RUN_PROCESSED,
                "PROCESS",
                "PAYROLL_RUN",
                run.getId(),
                actorUserId,
                "Payroll run processed; organizationId=%s; payrollMonth=%s; status=%s; entryCount=%d"
                        .formatted(run.getOrganizationId(), run.getPayrollMonth(), run.getStatus(), entryCount));
        publishAfterCommit(event);
    }

    public void publishPayrollRunFinalized(PayrollRun run, UUID actorUserId) {
        AuditEvent event = baseEvent(
                PAYROLL_RUN_FINALIZED,
                "FINALIZE",
                "PAYROLL_RUN",
                run.getId(),
                actorUserId,
                "Payroll run finalized; organizationId=%s; payrollMonth=%s; status=%s"
                        .formatted(run.getOrganizationId(), run.getPayrollMonth(), run.getStatus()));
        publishAfterCommit(event);
    }

    private AuditEvent baseEvent(
            String eventType,
            String action,
            String entityType,
            UUID entityId,
            UUID actorUserId,
            String details) {
        return AuditEvent.builder()
                .eventType(eventType)
                .serviceName("payroll-service")
                .userId(actorUserId.toString())
                .action(action)
                .entityType(entityType)
                .entityId(entityId.toString())
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void publishAfterCommit(AuditEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safePublish(event);
                }
            });
            return;
        }
        safePublish(event);
    }

    private void safePublish(AuditEvent event) {
        try {
            auditPublisher.publish(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to publish HRMS payroll audit event eventType={} entityType={} entityId={}",
                    event.getEventType(),
                    event.getEntityType(),
                    event.getEntityId(),
                    exception);
        }
    }
}
