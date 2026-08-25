package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.hrms.payroll.dto.PayrollTransitionRequest;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;
import com.ums.hrms.payroll.repository.StatutoryPolicyRepository;

@ExtendWith(MockitoExtension.class)
class PayrollSalaryVersionSelectionTests {

    @Mock PayrollRunRepository payrollRunRepository;
    @Mock PayrollEntryRepository payrollEntryRepository;
    @Mock SalaryStructureRepository salaryStructureRepository;
    @Mock StatutoryPolicyRepository statutoryPolicyRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;

    @Spy
    StatutoryPayrollCalculator statutoryPayrollCalculator = new StatutoryPayrollCalculator();

    @InjectMocks PayrollRunService payrollRunService;

    private UUID organizationId;
    private UUID actorUserId;
    private UUID employeeId;
    private SalaryStructure v1;
    private SalaryStructure v2;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        v1 = salaryStructure(
                1,
                new BigDecimal("50000.00"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30));
        v2 = salaryStructure(
                2,
                new BigDecimal("60000.00"),
                LocalDate.of(2026, 7, 1),
                null);
        v2.setSupersedesStructureId(v1.getId());
    }

    @Test
    void junePayrollSelectsVersionOneAtMonthEnd() {
        PayrollRun run = run(YearMonth.of(2026, 6));
        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(run.getId(), organizationId))
                .thenReturn(Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(v1));
        when(payrollRunRepository.save(run)).thenReturn(run);

        payrollRunService.process(
                run.getId(),
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        PayrollEntry entry = capturedEntry();
        assertEquals(v1.getId(), entry.getSalaryStructureId());
        assertEquals(new BigDecimal("50000.00"), entry.getBasicPay());
        verify(salaryStructureRepository).findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 6, 30));
    }

    @Test
    void julyPayrollSelectsVersionTwoAtMonthEnd() {
        PayrollRun run = run(YearMonth.of(2026, 7));
        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(run.getId(), organizationId))
                .thenReturn(Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(v2));
        when(payrollRunRepository.save(run)).thenReturn(run);

        payrollRunService.process(
                run.getId(),
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        PayrollEntry entry = capturedEntry();
        assertEquals(v2.getId(), entry.getSalaryStructureId());
        assertEquals(new BigDecimal("60000.00"), entry.getBasicPay());
        verify(salaryStructureRepository).findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 7, 31));
    }

    private PayrollRun run(YearMonth payrollMonth) {
        PayrollRun run = new PayrollRun();
        run.setId(UUID.randomUUID());
        run.setOrganizationId(organizationId);
        run.setPayrollMonth(payrollMonth);
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setCreatedBy(actorUserId);
        return run;
    }

    private SalaryStructure salaryStructure(
            int versionNumber,
            BigDecimal basicPay,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
        SalaryStructure structure = new SalaryStructure();
        structure.setId(UUID.randomUUID());
        structure.setOrganizationId(organizationId);
        structure.setEmployeeId(employeeId);
        structure.setVersionNumber(versionNumber);
        structure.setCurrency("INR");
        structure.setBasicPay(basicPay);
        structure.setAllowanceTotal(BigDecimal.ZERO.setScale(2));
        structure.setDeductionTotal(BigDecimal.ZERO.setScale(2));
        structure.setTdsAmount(BigDecimal.ZERO.setScale(2));
        structure.setEffectiveFrom(effectiveFrom);
        structure.setEffectiveTo(effectiveTo);
        structure.setActive(true);
        structure.setCreatedBy(actorUserId);
        return structure;
    }

    private PayrollEntry capturedEntry() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(payrollEntryRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        return captor.getValue().getFirst();
    }
}
