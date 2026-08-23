package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.client.EmployeeInternalResponse;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.TaxRegime;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;

@ExtendWith(MockitoExtension.class)
class PayslipPdfServiceTests {

    @Mock PayrollEntryRepository payrollEntryRepository;
    @Mock PayrollRunRepository payrollRunRepository;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock PayrollTenantValidationService payrollTenantValidationService;
    @InjectMocks PayslipPdfService payslipPdfService;

    private UUID organizationId;
    private UUID actorUserId;
    private UUID entryId;
    private UUID runId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        entryId = UUID.randomUUID();
        runId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    @Test
    void generatesFinalizedPdfFromPersistedSnapshot() throws Exception {
        PayrollEntry entry = entry();
        PayrollRun run = run(PayrollRunStatus.FINALIZED);
        run.setFinalizedAt(LocalDateTime.of(2026, 8, 31, 18, 30));

        when(payrollEntryRepository.findByIdAndOrganizationId(entryId, organizationId))
                .thenReturn(Optional.of(entry));
        when(payrollRunRepository.findByIdAndOrganizationId(runId, organizationId))
                .thenReturn(Optional.of(run));
        when(payrollTenantValidationService.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeInternalResponse(employeeId, organizationId, "EMP-001", "ACTIVE"));

        var document = payslipPdfService.generate(entryId, organizationId, actorUserId, false);

        assertEquals("payslip-EMP-001-2026-08.pdf", document.filename());
        assertTrue(document.content().length > 100);
        assertTrue(new String(document.content(), 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));

        try (PDDocument pdf = Loader.loadPDF(document.content())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(pdf);
            String normalizedText = text.replaceAll("\\s+", " ");
            assertTrue(text.contains("EMP-001"));
            assertTrue(text.contains("2026-08"));
            assertTrue(text.contains("50000.00"));
            assertTrue(text.contains("7500.00"));
            assertTrue(text.contains("57500.00"));

            assertTrue(text.contains("DEDUCTION BREAKDOWN"));
            assertTrue(text.contains("Configured / Other Deductions"));
            assertTrue(text.contains("500.00"));
            assertTrue(normalizedText.contains(
                    "Configured / Other Deductions: 500.00"));
            assertTrue(text.contains("Employee PF"));
            assertTrue(text.contains("1200.00"));
            assertTrue(text.contains("Employee ESI"));
            assertTrue(text.contains("135.00"));
            assertTrue(text.contains("TDS"));
            assertTrue(text.contains("665.00"));
            assertTrue(text.contains("Statutory Employee Deductions"));
            assertTrue(text.contains("2000.00"));
            assertTrue(normalizedText.contains(
                    "Statutory Employee Deductions: 2000.00"));
            assertTrue(text.contains("Total Deductions"));
            assertTrue(text.contains("2500.00"));
            assertTrue(text.contains("Net Pay"));
            assertTrue(text.contains("55000.00"));

            assertTrue(text.contains("EMPLOYER CONTRIBUTIONS"));
            assertTrue(text.contains("Employer PF"));
            assertTrue(text.contains("Employer ESI"));
            assertTrue(text.contains("585.00"));
            assertTrue(text.contains("Employer Statutory Total"));
            assertTrue(text.contains("1785.00"));

            assertTrue(text.contains("STATUTORY SNAPSHOT"));
            assertTrue(text.contains("Policy Version"));
            assertTrue(text.contains("IN-2026.1"));
            assertTrue(text.contains("Tax Regime"));
            assertTrue(text.contains("NEW"));
        }

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
    }

    @Test
    void rejectsOfficialPdfBeforeRunIsFinalized() {
        PayrollEntry entry = entry();
        PayrollRun run = run(PayrollRunStatus.PROCESSED);

        when(payrollEntryRepository.findByIdAndOrganizationId(entryId, organizationId))
                .thenReturn(Optional.of(entry));
        when(payrollRunRepository.findByIdAndOrganizationId(runId, organizationId))
                .thenReturn(Optional.of(run));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payslipPdfService.generate(entryId, organizationId, actorUserId, false));

        assertEquals(409, ex.getStatusCode().value());
        verify(payrollTenantValidationService, never()).getEmployee(employeeId, organizationId);
    }

    @Test
    void rejectsEntryOutsideRequestedOrganization() {
        when(payrollEntryRepository.findByIdAndOrganizationId(entryId, organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> payslipPdfService.generate(entryId, organizationId, actorUserId, false));

        assertEquals(404, ex.getStatusCode().value());
        verify(payrollRunRepository, never()).findByIdAndOrganizationId(runId, organizationId);
    }

    private PayrollEntry entry() {
        PayrollEntry entry = new PayrollEntry();
        entry.setId(entryId);
        entry.setPayrollRunId(runId);
        entry.setOrganizationId(organizationId);
        entry.setEmployeeId(employeeId);
        entry.setSalaryStructureId(UUID.randomUUID());
        entry.setBasicPay(new BigDecimal("50000.00"));
        entry.setAllowanceTotal(new BigDecimal("7500.00"));
        entry.setGrossPay(new BigDecimal("57500.00"));

        entry.setConfiguredDeductionTotal(new BigDecimal("500.00"));
        entry.setPfContributionWage(new BigDecimal("10000.00"));
        entry.setEmployeePfContribution(new BigDecimal("1200.00"));
        entry.setEmployerPfContribution(new BigDecimal("1200.00"));
        entry.setEsiContributionWage(new BigDecimal("18000.00"));
        entry.setEmployeeEsiContribution(new BigDecimal("135.00"));
        entry.setEmployerEsiContribution(new BigDecimal("585.00"));
        entry.setTdsAmount(new BigDecimal("665.00"));
        entry.setStatutoryEmployeeDeductionTotal(new BigDecimal("2000.00"));
        entry.setEmployerStatutoryContributionTotal(new BigDecimal("1785.00"));
        entry.setStatutoryPolicyId(UUID.randomUUID());
        entry.setStatutoryPolicyVersion("IN-2026.1");
        entry.setTaxRegime(TaxRegime.NEW);

        entry.setDeductionTotal(new BigDecimal("2500.00"));
        entry.setNetPay(new BigDecimal("55000.00"));
        entry.setGeneratedAt(LocalDateTime.of(2026, 8, 31, 12, 0));
        return entry;
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
}
