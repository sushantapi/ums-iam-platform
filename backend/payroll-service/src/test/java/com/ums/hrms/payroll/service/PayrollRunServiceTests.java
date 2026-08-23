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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreatePayrollRunRequest;
import com.ums.hrms.payroll.dto.PayrollTransitionRequest;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.StatutoryPolicy;
import com.ums.hrms.payroll.entity.TaxRegime;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;
import com.ums.hrms.payroll.repository.StatutoryPolicyRepository;

@ExtendWith(MockitoExtension.class)
class PayrollRunServiceTests {

    @Mock PayrollRunRepository payrollRunRepository;
    @Mock PayrollEntryRepository payrollEntryRepository;
    @Mock SalaryStructureRepository salaryStructureRepository;
    @Mock StatutoryPolicyRepository statutoryPolicyRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;

    @Spy
    StatutoryPayrollCalculator statutoryPayrollCalculator =
            new StatutoryPayrollCalculator();

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
        when(payrollRunRepository.existsByOrganizationIdAndPayrollMonth(
                organizationId,
                month))
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
    void processesDraftRunIntoImmutableMoneySnapshotsAndAudits() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);
        SalaryStructure structure = salaryStructure(
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"));

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(payrollRunRepository.save(run)).thenReturn(run);

        var response = payrollRunService.process(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        PayrollEntry entry = capturedEntry();

        assertEquals(new BigDecimal("50000.00"), entry.getBasicPay());
        assertEquals(new BigDecimal("5000.00"), entry.getAllowanceTotal());
        assertEquals(new BigDecimal("55000.00"), entry.getGrossPay());

        assertEquals(
                new BigDecimal("2500.00"),
                entry.getConfiguredDeductionTotal());
        assertEquals(
                new BigDecimal("0.00"),
                entry.getStatutoryEmployeeDeductionTotal());
        assertEquals(new BigDecimal("2500.00"), entry.getDeductionTotal());
        assertEquals(new BigDecimal("52500.00"), entry.getNetPay());

        assertNull(entry.getStatutoryPolicyId());
        assertNull(entry.getStatutoryPolicyVersion());

        assertEquals(PayrollRunStatus.PROCESSED, response.status());
        assertEquals(actorUserId, response.processedBy());

        verify(statutoryPolicyRepository, never())
                .findAllActiveEffectiveOn(any(), any(), any());
        verify(payrollAuditPublisher)
                .publishPayrollRunProcessed(run, actorUserId, 1);

        structure.setBasicPay(new BigDecimal("99999.00"));

        assertEquals(
                new BigDecimal("50000.00"),
                entry.getBasicPay());
    }

    @Test
    void snapshotsPfEsiTdsAndKeepsEmployerContributionsOutOfNetPay() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        SalaryStructure structure = salaryStructure(
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"));

        structure.setPfApplicable(true);
        structure.setPfContributionWage(new BigDecimal("20000.00"));
        structure.setEsiApplicable(true);
        structure.setEsiContributionWage(new BigDecimal("18000.00"));
        structure.setTdsAmount(new BigDecimal("500.00"));
        structure.setTaxRegime(TaxRegime.NEW);

        StatutoryPolicy policy = statutoryPolicy();

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(statutoryPolicyRepository.findAllActiveEffectiveOn(
                organizationId,
                "IN",
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(policy));
        when(payrollRunRepository.save(run)).thenReturn(run);

        payrollRunService.process(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        PayrollEntry entry = capturedEntry();

        assertEquals(policy.getId(), entry.getStatutoryPolicyId());
        assertEquals(
                "IN-2026.1",
                entry.getStatutoryPolicyVersion());

        assertEquals(
                new BigDecimal("15000.00"),
                entry.getPfContributionWage());
        assertEquals(
                new BigDecimal("1800.00"),
                entry.getEmployeePfContribution());
        assertEquals(
                new BigDecimal("1800.00"),
                entry.getEmployerPfContribution());

        assertEquals(
                new BigDecimal("18000.00"),
                entry.getEsiContributionWage());
        assertEquals(
                new BigDecimal("135.00"),
                entry.getEmployeeEsiContribution());
        assertEquals(
                new BigDecimal("585.00"),
                entry.getEmployerEsiContribution());

        assertEquals(
                new BigDecimal("500.00"),
                entry.getTdsAmount());

        assertEquals(
                new BigDecimal("2435.00"),
                entry.getStatutoryEmployeeDeductionTotal());

        assertEquals(
                new BigDecimal("2385.00"),
                entry.getEmployerStatutoryContributionTotal());

        assertEquals(
                new BigDecimal("2500.00"),
                entry.getConfiguredDeductionTotal());

        assertEquals(
                new BigDecimal("4935.00"),
                entry.getDeductionTotal());

        assertEquals(
                new BigDecimal("50065.00"),
                entry.getNetPay());

        assertEquals(TaxRegime.NEW, entry.getTaxRegime());

        policy.setPfEmployeeRate(new BigDecimal("0.990000"));
        structure.setTdsAmount(new BigDecimal("9999.00"));

        assertEquals(
                new BigDecimal("1800.00"),
                entry.getEmployeePfContribution());
        assertEquals(
                new BigDecimal("500.00"),
                entry.getTdsAmount());
        assertEquals(
                new BigDecimal("50065.00"),
                entry.getNetPay());

        verify(statutoryPolicyRepository)
                .findAllActiveEffectiveOn(
                        organizationId,
                        "IN",
                        LocalDate.of(2026, 8, 31));
    }

    @Test
    void processesTdsOnlyWithoutResolvingStatutoryPolicy() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        SalaryStructure structure = salaryStructure(
                new BigDecimal("10000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"));

        structure.setTdsAmount(new BigDecimal("1000.00"));
        structure.setTaxRegime(TaxRegime.OLD);

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(payrollRunRepository.save(run)).thenReturn(run);

        payrollRunService.process(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        PayrollEntry entry = capturedEntry();

        assertNull(entry.getStatutoryPolicyId());
        assertNull(entry.getStatutoryPolicyVersion());

        assertEquals(
                new BigDecimal("1000.00"),
                entry.getTdsAmount());
        assertEquals(
                new BigDecimal("1000.00"),
                entry.getStatutoryEmployeeDeductionTotal());
        assertEquals(
                new BigDecimal("1500.00"),
                entry.getDeductionTotal());
        assertEquals(
                new BigDecimal("9500.00"),
                entry.getNetPay());

        assertEquals(
                new BigDecimal("0.00"),
                entry.getEmployerStatutoryContributionTotal());

        verify(statutoryPolicyRepository, never())
                .findAllActiveEffectiveOn(any(), any(), any());
    }

    @Test
    void rejectsProcessingWhenRequiredStatutoryPolicyIsMissing() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        SalaryStructure structure = salaryStructure(
                new BigDecimal("50000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));

        structure.setPfApplicable(true);
        structure.setPfContributionWage(new BigDecimal("15000.00"));

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(statutoryPolicyRepository.findAllActiveEffectiveOn(
                organizationId,
                "IN",
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.process(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(
                "No active statutory policy found for payroll month",
                ex.getReason());

        assertEquals(PayrollRunStatus.DRAFT, run.getStatus());

        verify(payrollEntryRepository, never()).saveAll(anyList());
        verify(payrollAuditPublisher, never())
                .publishPayrollRunProcessed(
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rejectsProcessingWhenMultipleStatutoryPoliciesAreEffective() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        SalaryStructure structure = salaryStructure(
                new BigDecimal("50000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));

        structure.setEsiApplicable(true);
        structure.setEsiContributionWage(new BigDecimal("18000.00"));

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(statutoryPolicyRepository.findAllActiveEffectiveOn(
                organizationId,
                "IN",
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(
                        statutoryPolicy(),
                        statutoryPolicy()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.process(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(
                "Multiple active statutory policies found for payroll month",
                ex.getReason());

        verify(payrollEntryRepository, never()).saveAll(anyList());
        verify(payrollAuditPublisher, never())
                .publishPayrollRunProcessed(
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rejectsNegativeNetPayAfterStatutoryDeductions() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        SalaryStructure structure = salaryStructure(
                new BigDecimal("1000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));

        structure.setTdsAmount(new BigDecimal("1200.00"));

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
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
        verify(payrollAuditPublisher, never())
                .publishPayrollRunProcessed(
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void exposesPersistedStatutorySnapshotInPayslipJson() {
        UUID entryId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID salaryStructureId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        PayrollEntry entry = new PayrollEntry();
        entry.setId(entryId);
        entry.setPayrollRunId(runId);
        entry.setOrganizationId(organizationId);
        entry.setEmployeeId(employeeId);
        entry.setSalaryStructureId(salaryStructureId);
        entry.setBasicPay(new BigDecimal("50000.00"));
        entry.setAllowanceTotal(new BigDecimal("5000.00"));
        entry.setGrossPay(new BigDecimal("55000.00"));
        entry.setConfiguredDeductionTotal(new BigDecimal("2500.00"));
        entry.setPfContributionWage(new BigDecimal("15000.00"));
        entry.setEmployeePfContribution(new BigDecimal("1800.00"));
        entry.setEmployerPfContribution(new BigDecimal("1800.00"));
        entry.setEsiContributionWage(new BigDecimal("18000.00"));
        entry.setEmployeeEsiContribution(new BigDecimal("135.00"));
        entry.setEmployerEsiContribution(new BigDecimal("585.00"));
        entry.setTdsAmount(new BigDecimal("500.00"));
        entry.setStatutoryEmployeeDeductionTotal(new BigDecimal("2435.00"));
        entry.setEmployerStatutoryContributionTotal(new BigDecimal("2385.00"));
        entry.setStatutoryPolicyId(policyId);
        entry.setStatutoryPolicyVersion("IN-2026.1");
        entry.setTaxRegime(TaxRegime.NEW);
        entry.setDeductionTotal(new BigDecimal("4935.00"));
        entry.setNetPay(new BigDecimal("50065.00"));
        entry.setGeneratedAt(java.time.LocalDateTime.of(2026, 8, 31, 12, 0));

        when(payrollEntryRepository.findByIdAndOrganizationId(
                entryId,
                organizationId))
                .thenReturn(java.util.Optional.of(entry));

        var response = payrollRunService.getPayslip(
                entryId,
                organizationId,
                actorUserId,
                false);

        assertEquals(entryId, response.id());
        assertEquals(runId, response.payrollRunId());
        assertEquals(employeeId, response.employeeId());
        assertEquals(salaryStructureId, response.salaryStructureId());

        assertEquals(new BigDecimal("2500.00"), response.configuredDeductionTotal());
        assertEquals(new BigDecimal("15000.00"), response.pfContributionWage());
        assertEquals(new BigDecimal("1800.00"), response.employeePfContribution());
        assertEquals(new BigDecimal("1800.00"), response.employerPfContribution());
        assertEquals(new BigDecimal("18000.00"), response.esiContributionWage());
        assertEquals(new BigDecimal("135.00"), response.employeeEsiContribution());
        assertEquals(new BigDecimal("585.00"), response.employerEsiContribution());
        assertEquals(new BigDecimal("500.00"), response.tdsAmount());
        assertEquals(new BigDecimal("2435.00"), response.statutoryEmployeeDeductionTotal());
        assertEquals(new BigDecimal("2385.00"), response.employerStatutoryContributionTotal());

        assertEquals(policyId, response.statutoryPolicyId());
        assertEquals("IN-2026.1", response.statutoryPolicyVersion());
        assertEquals(TaxRegime.NEW, response.taxRegime());

        assertEquals(new BigDecimal("4935.00"), response.deductionTotal());
        assertEquals(new BigDecimal("50065.00"), response.netPay());

        verify(organizationAccessService)
                .assertCanAccess(organizationId, actorUserId, false);
    }

    @Test
    void rejectsProcessingWhenRunIsNotDraft() {
        PayrollRun run = run(PayrollRunStatus.PROCESSED);

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payrollRunService.process(
                        runId,
                        new PayrollTransitionRequest(organizationId),
                        actorUserId,
                        false));

        assertEquals(409, ex.getStatusCode().value());

        verify(salaryStructureRepository, never())
                .findAllActiveEffectiveOn(any(), any());
    }

    @Test
    void rejectsProcessingWhenNoSalaryStructuresApplyAtMonthEnd() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
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
    void finalizesOnlyProcessedRunAndAudits() {
        PayrollRun run = run(PayrollRunStatus.PROCESSED);

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(payrollRunRepository.save(run)).thenReturn(run);

        var response = payrollRunService.finalizeRun(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        assertEquals(PayrollRunStatus.FINALIZED, response.status());
        assertEquals(actorUserId, response.finalizedBy());

        verify(payrollAuditPublisher)
                .publishPayrollRunFinalized(run, actorUserId);
    }

    @Test
    void rejectsFinalizeFromDraftWithoutAudit() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(
                runId,
                organizationId))
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

        verify(payrollAuditPublisher, never())
                .publishPayrollRunFinalized(any(), any());
    }

    private PayrollEntry capturedEntry() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollEntry>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(payrollEntryRepository).saveAll(captor.capture());

        return captor.getValue().getFirst();
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
        structure.setTdsAmount(BigDecimal.ZERO);
        structure.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        structure.setActive(true);
        return structure;
    }

    private StatutoryPolicy statutoryPolicy() {
        StatutoryPolicy policy = new StatutoryPolicy();
        policy.setId(UUID.randomUUID());
        policy.setOrganizationId(organizationId);
        policy.setCountryCode("IN");
        policy.setPolicyVersion("IN-2026.1");
        policy.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        policy.setActive(true);

        policy.setPfEmployeeRate(new BigDecimal("0.120000"));
        policy.setPfEmployerRate(new BigDecimal("0.120000"));
        policy.setPfContributionWageCeiling(
                new BigDecimal("15000.00"));

        policy.setEsiEmployeeRate(new BigDecimal("0.007500"));
        policy.setEsiEmployerRate(new BigDecimal("0.032500"));
        policy.setEsiWageEligibilityCeiling(
                new BigDecimal("21000.00"));

        return policy;
    }
}
