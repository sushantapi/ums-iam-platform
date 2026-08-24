package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.hrms.payroll.client.EmployeeInternalResponse;
import com.ums.hrms.payroll.client.OrganizationProfileInternalResponse;
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
class PayrollRunPresentationSnapshotTests {

    @Mock PayrollRunRepository payrollRunRepository;
    @Mock PayrollEntryRepository payrollEntryRepository;
    @Mock SalaryStructureRepository salaryStructureRepository;
    @Mock StatutoryPolicyRepository statutoryPolicyRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollAuditPublisher payrollAuditPublisher;

    @Spy
    StatutoryPayrollCalculator statutoryPayrollCalculator = new StatutoryPayrollCalculator();

    @InjectMocks PayrollRunService payrollRunService;

    @Test
    void snapshotsOrganizationAndEmployeePresentationDataWhenRunIsProcessed() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID logoAssetId = UUID.randomUUID();

        PayrollRun run = new PayrollRun();
        run.setId(runId);
        run.setOrganizationId(organizationId);
        run.setPayrollMonth(YearMonth.of(2026, 8));
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setCreatedBy(actorUserId);

        SalaryStructure structure = new SalaryStructure();
        structure.setId(structureId);
        structure.setOrganizationId(organizationId);
        structure.setEmployeeId(employeeId);
        structure.setBasicPay(new BigDecimal("50000.00"));
        structure.setAllowanceTotal(new BigDecimal("5000.00"));
        structure.setDeductionTotal(new BigDecimal("2500.00"));
        structure.setTdsAmount(BigDecimal.ZERO);
        structure.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        structure.setActive(true);

        OrganizationProfileInternalResponse profile = new OrganizationProfileInternalResponse(
                organizationId,
                "Acme Technologies Private Limited",
                "Acme Technologies",
                "100 Business Park, Bengaluru",
                "payroll@acme.example",
                "+91-9999999999",
                "https://acme.example",
                "INR",
                "IN",
                "This is a system-generated payslip.",
                "Authorized Signatory",
                logoAssetId,
                3);

        EmployeeInternalResponse employee = new EmployeeInternalResponse(
                employeeId,
                organizationId,
                "EMP-001",
                "Sushant Kumar",
                LocalDate.of(2025, 9, 24),
                "Engineering",
                "Backend Engineer",
                "******234F",
                "********0400",
                "******7890",
                "********7890",
                "ACTIVE");

        when(payrollRunRepository.findByIdAndOrganizationIdForUpdate(runId, organizationId))
                .thenReturn(java.util.Optional.of(run));
        when(salaryStructureRepository.findAllActiveEffectiveOn(
                organizationId,
                LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(structure));
        when(organizationAccessService.getProfile(organizationId)).thenReturn(profile);
        when(organizationAccessService.getEmployeePresentation(employeeId, organizationId)).thenReturn(employee);
        when(payrollRunRepository.save(run)).thenReturn(run);

        payrollRunService.process(
                runId,
                new PayrollTransitionRequest(organizationId),
                actorUserId,
                false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(payrollEntryRepository).saveAll(captor.capture());
        PayrollEntry entry = captor.getValue().getFirst();

        assertEquals("Acme Technologies Private Limited", entry.getOrganizationLegalName());
        assertEquals("Acme Technologies", entry.getOrganizationDisplayName());
        assertEquals("100 Business Park, Bengaluru", entry.getOrganizationRegisteredAddress());
        assertEquals("payroll@acme.example", entry.getOrganizationBusinessEmail());
        assertEquals("INR", entry.getOrganizationDefaultCurrency());
        assertEquals("IN", entry.getOrganizationPayrollCountry());
        assertEquals(logoAssetId, entry.getOrganizationLogoAssetId());
        assertEquals(3, entry.getOrganizationLogoAssetVersion());

        assertEquals("EMP-001", entry.getEmployeeCodeSnapshot());
        assertEquals("Sushant Kumar", entry.getEmployeeDisplayName());
        assertEquals(LocalDate.of(2025, 9, 24), entry.getEmployeeDateOfJoining());
        assertEquals("Engineering", entry.getEmployeeDepartmentName());
        assertEquals("Backend Engineer", entry.getEmployeeDesignationName());
        assertEquals("******234F", entry.getEmployeePanDisplay());
        assertEquals("********7890", entry.getEmployeeBankAccountDisplay());

        verify(payrollEntryRepository).saveAll(anyList());
        verify(payrollAuditPublisher).publishPayrollRunProcessed(run, actorUserId, 1);
    }
}
