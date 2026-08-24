package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.hrms.payroll.dto.PayrollTransitionRequest;
import com.ums.hrms.payroll.dto.SupersedeSalaryStructureRequest;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.TaxRegime;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;
import com.ums.hrms.payroll.repository.StatutoryPolicyRepository;

@ExtendWith(MockitoExtension.class)
class PayrollSalaryVersionImmutabilityTests {

    @Mock PayrollRunRepository payrollRunRepository;
    @Mock PayrollEntryRepository payrollEntryRepository;
    @Mock SalaryStructureRepository salaryStructureRepository;
    @Mock StatutoryPolicyRepository statutoryPolicyRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollTenantValidationService employeeValidationService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;

    private PayrollRunService payrollRunService;
    private SalaryStructureService salaryStructureService;
    private UUID organizationId;
    private UUID actorUserId;
    private UUID employeeId;
    private SalaryStructure versionOne;

    @BeforeEach
    void setUp() {
        payrollRunService = new PayrollRunService(
                payrollRunRepository,
                payrollEntryRepository,
                salaryStructureRepository,
                statutoryPolicyRepository,
                organizationAccessService,
                payrollAuditPublisher,
                new StatutoryPayrollCalculator());
        salaryStructureService = new SalaryStructureService(
                salaryStructureRepository,
                organizationAccessService,
                employeeValidationService,
                payrollAuditPublisher);

        organizationId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        versionOne = new SalaryStructure();
        versionOne.setId(UUID.randomUUID());
        versionOne.setOrganizationId(organizationId);
        versionOne.setEmployeeId(employeeId);
        versionOne.setVersionNumber(1);
        versionOne.setCurrency("INR");
        versionOne.setBasicPay(new BigDecimal("50000.00"));
        versionOne.setAllowanceTotal(new BigDecimal("5000.00"));
        versionOne.setDeductionTotal(new BigDecimal("1000.00"));
        versionOne.setTdsAmount(BigDecimal.ZERO.setScale(2));
        versionOne.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        versionOne.setActive(true);
        versionOne.setCreatedBy(actorUserId);
    }

    @Test
    void laterSalarySupersedeDoesNotChangeFinalizedPayrollSnapshot() {
        PayrollRun run = new PayrollRun();
        run.setId(UUID.randomUUID());
        run.setOrganizationId(organizationId);
        run.setPayrollMonth(YearMonth.of(2026, 6));
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setCreatedBy(actorUserId);

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(run.getId(), organizationId))
                .thenReturn(Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(versionOne));
        when(payrollRunRepository.save(run)).thenReturn(run);

        payrollRunService.process(
                run.getId(),
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        PayrollEntry finalizedSnapshot = capturedEntry();

        payrollRunService.finalizeRun(
                run.getId(),
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        assertEquals(PayrollRunStatus.FINALIZED, run.getStatus());
        assertEquals(versionOne.getId(), finalizedSnapshot.getSalaryStructureId());
        assertEquals(new BigDecimal("50000.00"), finalizedSnapshot.getBasicPay());
        assertEquals(new BigDecimal("5000.00"), finalizedSnapshot.getAllowanceTotal());
        assertEquals(new BigDecimal("55000.00"), finalizedSnapshot.getGrossPay());
        assertEquals(new BigDecimal("1000.00"), finalizedSnapshot.getDeductionTotal());
        assertEquals(new BigDecimal("54000.00"), finalizedSnapshot.getNetPay());

        when(salaryStructureRepository.findByIdAndOrganizationIdForUpdate(
                versionOne.getId(),
                organizationId))
                .thenReturn(Optional.of(versionOne));
        when(salaryStructureRepository.existsBySupersedesStructureId(versionOne.getId()))
                .thenReturn(false);
        when(salaryStructureRepository.countOverlappingEffectiveRangesExcludingId(
                organizationId,
                employeeId,
                versionOne.getId(),
                LocalDate.of(2026, 7, 1),
                null))
                .thenReturn(0L);
        when(salaryStructureRepository.save(any(SalaryStructure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(salaryStructureRepository.saveAndFlush(any(SalaryStructure.class)))
                .thenAnswer(invocation -> {
                    SalaryStructure successor = invocation.getArgument(0);
                    successor.setId(UUID.randomUUID());
                    return successor;
                });

        var successor = salaryStructureService.supersede(
                versionOne.getId(),
                new SupersedeSalaryStructureRequest(
                        organizationId,
                        "INR",
                        new BigDecimal("60000.00"),
                        new BigDecimal("7000.00"),
                        new BigDecimal("1200.00"),
                        false,
                        null,
                        false,
                        null,
                        new BigDecimal("500.00"),
                        TaxRegime.NEW,
                        LocalDate.of(2026, 7, 1)),
                actorUserId,
                false);

        assertEquals(2, successor.versionNumber());
        assertEquals(versionOne.getId(), successor.supersedesStructureId());
        assertEquals(LocalDate.of(2026, 6, 30), versionOne.getEffectiveTo());

        assertEquals(PayrollRunStatus.FINALIZED, run.getStatus());
        assertEquals(versionOne.getId(), finalizedSnapshot.getSalaryStructureId());
        assertEquals(new BigDecimal("50000.00"), finalizedSnapshot.getBasicPay());
        assertEquals(new BigDecimal("5000.00"), finalizedSnapshot.getAllowanceTotal());
        assertEquals(new BigDecimal("55000.00"), finalizedSnapshot.getGrossPay());
        assertEquals(new BigDecimal("1000.00"), finalizedSnapshot.getDeductionTotal());
        assertEquals(new BigDecimal("54000.00"), finalizedSnapshot.getNetPay());
    }

    private PayrollEntry capturedEntry() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(payrollEntryRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        return captor.getValue().getFirst();
    }
}
