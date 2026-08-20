package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.SalaryStructure;

@ExtendWith(MockitoExtension.class)
class PayrollAuditPublisherTests {

    @Mock AuditPublisher auditPublisher;
    @InjectMocks PayrollAuditPublisher payrollAuditPublisher;

    @Test
    void publishesSalaryStructureCreatedWithoutMonetaryValues() {
        UUID actor = UUID.randomUUID();
        SalaryStructure structure = salaryStructure();

        payrollAuditPublisher.publishSalaryStructureCreated(structure, actor);

        AuditEvent event = capture();
        assertEquals(PayrollAuditPublisher.SALARY_STRUCTURE_CREATED, event.getEventType());
        assertEquals("payroll-service", event.getServiceName());
        assertEquals(actor.toString(), event.getUserId());
        assertEquals("SALARY_STRUCTURE", event.getEntityType());
        assertEquals(structure.getId().toString(), event.getEntityId());
        assertTrue(event.getDetails().contains("organizationId=" + structure.getOrganizationId()));
        assertTrue(!event.getDetails().contains("50000"));
    }

    @Test
    void publishesProcessedRunWithEntryCount() {
        UUID actor = UUID.randomUUID();
        PayrollRun run = run(PayrollRunStatus.PROCESSED);

        payrollAuditPublisher.publishPayrollRunProcessed(run, actor, 3);

        AuditEvent event = capture();
        assertEquals(PayrollAuditPublisher.PAYROLL_RUN_PROCESSED, event.getEventType());
        assertEquals("PROCESS", event.getAction());
        assertTrue(event.getDetails().contains("entryCount=3"));
    }

    @Test
    void publishesFinalizedRun() {
        UUID actor = UUID.randomUUID();
        PayrollRun run = run(PayrollRunStatus.FINALIZED);

        payrollAuditPublisher.publishPayrollRunFinalized(run, actor);

        AuditEvent event = capture();
        assertEquals(PayrollAuditPublisher.PAYROLL_RUN_FINALIZED, event.getEventType());
        assertEquals("FINALIZE", event.getAction());
        assertTrue(event.getDetails().contains("status=FINALIZED"));
    }

    @Test
    void rabbitFailureDoesNotBreakCommittedBusinessFlow() {
        SalaryStructure structure = salaryStructure();
        doThrow(new IllegalStateException("rabbit unavailable")).when(auditPublisher).publish(org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(() -> payrollAuditPublisher.publishSalaryStructureCreated(structure, UUID.randomUUID()));
    }

    private AuditEvent capture() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPublisher).publish(captor.capture());
        return captor.getValue();
    }

    private SalaryStructure salaryStructure() {
        SalaryStructure structure = new SalaryStructure();
        structure.setId(UUID.randomUUID());
        structure.setOrganizationId(UUID.randomUUID());
        structure.setEmployeeId(UUID.randomUUID());
        structure.setCurrency("INR");
        structure.setBasicPay(new java.math.BigDecimal("50000.00"));
        structure.setAllowanceTotal(new java.math.BigDecimal("5000.00"));
        structure.setDeductionTotal(new java.math.BigDecimal("2500.00"));
        structure.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        structure.setActive(true);
        return structure;
    }

    private PayrollRun run(PayrollRunStatus status) {
        PayrollRun run = new PayrollRun();
        run.setId(UUID.randomUUID());
        run.setOrganizationId(UUID.randomUUID());
        run.setPayrollMonth(YearMonth.of(2026, 8));
        run.setStatus(status);
        run.setCreatedBy(UUID.randomUUID());
        return run;
    }
}
