package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreatePayrollRunRequest;
import com.ums.hrms.payroll.dto.PayrollTransitionRequest;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class PayrollRunServiceTests {

    @Mock PayrollRunRepository payrollRunRepository;
    @Mock PayrollEntryRepository payrollEntryRepository;
    @Mock SalaryStructureRepository salaryStructureRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @InjectMocks PayrollRunService payrollRunService;

    private UUID organizationId;
    private UUID actorUserId;
    private UUID runId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        runId = UUID.randomUUID();
    }

    @Test
    void rejectsDuplicateOrganizationMonthOnCreate() {
        YearMonth month = YearMonth.of(2026, 8);
        when(payrollRunRepository.existsByOrganizationIdAndPayrollMonth(organizationId, month))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.create(
                        new CreatePayrollRunRequest(organizationId, month),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        verify(payrollRunRepository, never()).saveAndFlush(any());
    }

    @Test
    void processesDraftRunIntoImmutableMoneySnapshots() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);
        SalaryStructure structure = salaryStructure(
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"));

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId, LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(payrollRunRepository.save(run)).thenReturn(run);

        var response = payrollRunService.process(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(payrollEntryRepository).saveAll(captor.capture());
        PayrollEntry entry = captor.getValue().getFirst();

        assertEquals(new BigDecimal("50000.00"), entry.getBasicPay());
        assertEquals(new BigDecimal("5000.00"), entry.getAllowanceTotal());
        assertEquals(new BigDecimal("55000.00"), entry.getGrossPay());
        assertEquals(new BigDecimal("2500.00"), entry.getDeductionTotal());
        assertEquals(new BigDecimal("52500.00"), entry.getNetPay());
        assertEquals(PayrollRunStatus.PROCESSED, response.status());
        assertEquals(actorUserId, response.processedBy());

        structure.setBasicPay(new BigDecimal("99999.00"));
        assertEquals(new BigDecimal("50000.00"), entry.getBasicPay());
    }

    @Test
    void rejectsNegativeNetPayWithoutPersistingEntries() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);
        SalaryStructure structure = salaryStructure(
                new BigDecimal("1000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("1200.00"));

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId, LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.process(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(PayrollRunStatus.DRAFT, run.getStatus());
        assertNull(run.getProcessedBy());
        verify(payrollEntryRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsProcessingWhenRunIsNotDraft() {
        PayrollRun run = run(PayrollRunStatus.PROCESSED);
        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.process(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        verify(salaryStructureRepository, never()).findAllActiveEffectiveOn(any(), any());
    }

    @Test
    void rejectsProcessingWhenNoSalaryStructuresApplyAtMonthEnd() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);
        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId, LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.process(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        verify(payrollEntryRepository, never()).saveAll(anyList());
    }

    @Test
    void finalizesOnlyProcessedRun() {
        PayrollRun run = run(PayrollRunStatus.PROCESSED);
        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(payrollRunRepository.save(run)).thenReturn(run);

        var response = payrollRunService.finalizeRun(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        assertEquals(PayrollRunStatus.FINALIZED, response.status());
        assertEquals(actorUserId, response.finalizedBy());
    }

    @Test
    void rejectsFinalizeFromDraft() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);
        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.finalizeRun(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(PayrollRunStatus.DRAFT, run.getStatus());
    }

    private PayrollRun run(PayrollRunStatus status) {
        PayrollRun run = new PayrollRun();
        run.setId(runId);
        run.setOrganizationId(organizationId);
        run.setPayrollMonth(YearMonth.of(2026, 8));
        run.setStatus(status);
        run.setCreatedBy(UUID.randomUUID());
        return run;
    }

    private SalaryStructure salaryStructure(
            BigDecimal basic,
            BigDecimal allowance,
            BigDecimal deduction) {
        SalaryStructure structure = new SalaryStructure();
        structure.setId(UUID.randomUUID());
        structure.setOrganizationId(organizationId);
        structure.setEmployeeId(UUID.randomUUID());
        structure.setBasicPay(basic);
        structure.setAllowanceTotal(allowance);
        structure.setDeductionTotal(deduction);
        structure.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        structure.setActive(true);
        return structure;
    }
}
