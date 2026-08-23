package com.ums.hrms.payroll.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.client.EmployeeInternalResponse;
import com.ums.hrms.payroll.dto.PayslipPdfDocument;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayslipPdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float LEFT = 54f;
    private static final float TOP = 790f;
    private static final float LINE_HEIGHT = 20f;
    private static final float DEFAULT_VALUE_X = 190f;
    private static final float LABEL_VALUE_GAP = 12f;
    private static final float FIELD_FONT_SIZE = 11f;

    private final PayrollEntryRepository payrollEntryRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final OrganizationAccessService organizationAccessService;
    private final PayrollTenantValidationService payrollTenantValidationService;

    @Transactional(readOnly = true)
    public PayslipPdfDocument generate(
            UUID entryId,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);

        PayrollEntry entry = payrollEntryRepository.findByIdAndOrganizationId(entryId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll entry not found"));
        PayrollRun run = payrollRunRepository.findByIdAndOrganizationId(entry.getPayrollRunId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run not found"));

        if (run.getStatus() != PayrollRunStatus.FINALIZED || run.getFinalizedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Official payslip PDF is available only after payroll finalization");
        }

        EmployeeInternalResponse employee = payrollTenantValidationService.getEmployee(
                entry.getEmployeeId(), organizationId);
        String employeeCode = safeEmployeeCode(employee.employeeCode(), entry.getEmployeeId());

        return new PayslipPdfDocument(
                render(entry, run, employeeCode),
                "payslip-" + filenamePart(employeeCode) + "-" + run.getPayrollMonth() + ".pdf");
    }

    private byte[] render(PayrollEntry entry, PayrollRun run, String employeeCode) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = TOP;
                y = text(stream, bold, 20, LEFT, y, "UMS HRMS - PAYSLIP");
                y -= 8;
                y = text(stream, regular, 10, LEFT, y,
                        "Official finalized payroll snapshot. Amounts are rendered from persisted payroll entry values.");
                y -= 16;

                y = field(stream, bold, regular, y, "Payslip Reference", entry.getId().toString());
                y = field(stream, bold, regular, y, "Payroll Run", run.getId().toString());
                y = field(stream, bold, regular, y, "Payroll Month", run.getPayrollMonth().toString());
                y = field(stream, bold, regular, y, "Organization ID", entry.getOrganizationId().toString());
                y = field(stream, bold, regular, y, "Employee Code", employeeCode);
                y = field(stream, bold, regular, y, "Employee ID", entry.getEmployeeId().toString());
                y -= 14;

                y = text(stream, bold, 14, LEFT, y, "PAY DETAILS");
                y -= 6;
                y = moneyField(stream, bold, regular, y, "Basic Pay", entry.getBasicPay());
                y = moneyField(stream, bold, regular, y, "Allowances", entry.getAllowanceTotal());
                y = moneyField(stream, bold, regular, y, "Gross Pay", entry.getGrossPay());
                y -= 10;

                y = text(stream, bold, 14, LEFT, y, "DEDUCTION BREAKDOWN");
                y -= 6;
                y = moneyField(
                        stream, bold, regular, y,
                        "Configured / Other Deductions",
                        entry.getConfiguredDeductionTotal());
                y = moneyField(
                        stream, bold, regular, y,
                        "Employee PF",
                        entry.getEmployeePfContribution());
                y = moneyField(
                        stream, bold, regular, y,
                        "Employee ESI",
                        entry.getEmployeeEsiContribution());
                y = moneyField(stream, bold, regular, y, "TDS", entry.getTdsAmount());
                y = moneyField(
                        stream, bold, regular, y,
                        "Statutory Employee Deductions",
                        entry.getStatutoryEmployeeDeductionTotal());
                y = moneyField(
                        stream, bold, regular, y,
                        "Total Deductions",
                        entry.getDeductionTotal());
                y = moneyField(stream, bold, regular, y, "Net Pay", entry.getNetPay());
                y -= 10;

                y = text(stream, bold, 14, LEFT, y, "EMPLOYER CONTRIBUTIONS");
                y -= 6;
                y = moneyField(
                        stream, bold, regular, y,
                        "Employer PF",
                        entry.getEmployerPfContribution());
                y = moneyField(
                        stream, bold, regular, y,
                        "Employer ESI",
                        entry.getEmployerEsiContribution());
                y = moneyField(
                        stream, bold, regular, y,
                        "Employer Statutory Total",
                        entry.getEmployerStatutoryContributionTotal());
                y -= 10;

                y = text(stream, bold, 14, LEFT, y, "STATUTORY SNAPSHOT");
                y -= 6;
                y = field(
                        stream, bold, regular, y,
                        "Policy Version",
                        entry.getStatutoryPolicyVersion());
                y = field(
                        stream, bold, regular, y,
                        "Tax Regime",
                        entry.getTaxRegime() == null ? "" : entry.getTaxRegime().name());
                y -= 14;

                y = text(stream, bold, 14, LEFT, y, "AUDIT DETAILS");
                y -= 6;
                y = field(stream, bold, regular, y, "Generated At", DATE_TIME.format(entry.getGeneratedAt()));
                field(stream, bold, regular, y, "Finalized At", DATE_TIME.format(run.getFinalizedAt()));
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payslip PDF could not be generated", ex);
        }
    }

    private float moneyField(
            PDPageContentStream stream,
            PDType1Font bold,
            PDType1Font regular,
            float y,
            String label,
            BigDecimal amount) throws IOException {
        return field(stream, bold, regular, y, label, formatAmount(amount));
    }

    private float field(
            PDPageContentStream stream,
            PDType1Font bold,
            PDType1Font regular,
            float y,
            String label,
            String value) throws IOException {
        String renderedLabel = label + ":";
        float labelWidth =
                bold.getStringWidth(renderedLabel) / 1000f * FIELD_FONT_SIZE;
        float valueX = Math.max(
                DEFAULT_VALUE_X,
                LEFT + labelWidth + LABEL_VALUE_GAP);

        text(stream, bold, FIELD_FONT_SIZE, LEFT, y, renderedLabel);
        text(stream, regular, FIELD_FONT_SIZE, valueX, y, sanitizePdfText(value));
        return y - LINE_HEIGHT;
    }

    private float text(
            PDPageContentStream stream,
            PDType1Font font,
            float size,
            float x,
            float y,
            String value) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitizePdfText(value));
        stream.endText();
        return y - LINE_HEIGHT;
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safeEmployeeCode(String employeeCode, UUID employeeId) {
        if (employeeCode == null || employeeCode.isBlank()) {
            return employeeId.toString();
        }
        return employeeCode.trim();
    }

    private String filenamePart(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "employee" : safe;
    }

    private String sanitizePdfText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            safe.append(ch >= 32 && ch <= 126 ? ch : '?');
        }
        return safe.toString();
    }
}
