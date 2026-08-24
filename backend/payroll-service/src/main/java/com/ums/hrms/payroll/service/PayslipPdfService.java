package com.ums.hrms.payroll.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private static final float LEFT = 54f;
    private static final float RIGHT = 541f;
    private static final float CONTENT_WIDTH = RIGHT - LEFT;
    private static final float MID = LEFT + CONTENT_WIDTH / 2f;

    private final PayrollEntryRepository payrollEntryRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final OrganizationAccessService organizationAccessService;
    private final PayrollTenantValidationService payrollTenantValidationService;
    private final AmountInWordsFormatter amountInWordsFormatter;

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

        String employeeCode = snapshotEmployeeCode(entry);
        if (employeeCode == null) {
            EmployeeInternalResponse legacyEmployee = payrollTenantValidationService.getEmployee(
                    entry.getEmployeeId(), organizationId);
            employeeCode = safeEmployeeCode(legacyEmployee.employeeCode(), entry.getEmployeeId());
        }

        byte[] logoBytes = null;
        if (entry.getOrganizationLogoAssetId() != null) {
            logoBytes = organizationAccessService.getLogoAsset(
                    organizationId,
                    entry.getOrganizationLogoAssetId());
        }

        return new PayslipPdfDocument(
                render(entry, run, employeeCode, logoBytes),
                "payslip-" + filenamePart(employeeCode) + "-" + run.getPayrollMonth() + ".pdf");
    }

    private byte[] render(
            PayrollEntry entry,
            PayrollRun run,
            String employeeCode,
            byte[] logoBytes) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                drawHeader(document, stream, regular, bold, entry, run, logoBytes);
                drawEmployeeDetails(stream, regular, bold, entry, run, employeeCode);
                drawPayTable(stream, regular, bold, entry);
                drawNetPay(stream, regular, bold, entry);
                drawStatutorySummary(stream, regular, bold, entry);
                drawFooter(stream, regular, bold, entry, run);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payslip PDF could not be generated", ex);
        }
    }

    private void drawHeader(
            PDDocument document,
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            PayrollEntry entry,
            PayrollRun run,
            byte[] logoBytes) throws IOException {
        float companyX = LEFT;
        if (logoBytes != null && logoBytes.length > 0) {
            PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoBytes, "organization-logo");
            float maxWidth = 58f;
            float maxHeight = 46f;
            float scale = Math.min(maxWidth / logo.getWidth(), maxHeight / logo.getHeight());
            float width = logo.getWidth() * scale;
            float height = logo.getHeight() * scale;
            stream.drawImage(logo, LEFT, 746f, width, height);
            companyX = LEFT + 70f;
        }

        String displayName = firstNonBlank(
                entry.getOrganizationDisplayName(),
                entry.getOrganizationLegalName(),
                "UMS HRMS");
        String legalName = entry.getOrganizationLegalName();

        text(stream, bold, 16f, companyX, 790f, displayName);
        if (hasText(legalName) && !legalName.equalsIgnoreCase(displayName)) {
            text(stream, regular, 9f, companyX, 773f, legalName);
        }

        float detailY = hasText(legalName) && !legalName.equalsIgnoreCase(displayName) ? 758f : 772f;
        if (hasText(entry.getOrganizationRegisteredAddress())) {
            detailY = wrappedText(
                    stream,
                    regular,
                    8.5f,
                    companyX,
                    detailY,
                    RIGHT - companyX,
                    10f,
                    entry.getOrganizationRegisteredAddress(),
                    2);
        }

        String contact = joinNonBlank(
                " | ",
                entry.getOrganizationBusinessEmail(),
                entry.getOrganizationBusinessPhone(),
                entry.getOrganizationWebsite());
        if (hasText(contact)) {
            text(stream, regular, 8.5f, companyX, detailY, contact);
        }

        line(stream, LEFT, 732f, RIGHT, 732f);
        centeredText(
                stream,
                bold,
                15f,
                LEFT,
                RIGHT,
                710f,
                "PAYSLIP - " + run.getPayrollMonth().format(MONTH).toUpperCase(Locale.ENGLISH));
    }

    private void drawEmployeeDetails(
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            PayrollEntry entry,
            PayrollRun run,
            String employeeCode) throws IOException {
        text(stream, bold, 11f, LEFT, 688f, "EMPLOYEE DETAILS");

        float top = 676f;
        float bottom = 556f;
        float rowHeight = (top - bottom) / 5f;
        rectangle(stream, LEFT, bottom, CONTENT_WIDTH, top - bottom);
        line(stream, MID, bottom, MID, top);
        for (int row = 1; row < 5; row++) {
            float y = top - row * rowHeight;
            line(stream, LEFT, y, RIGHT, y);
        }

        String[] leftLabels = {"Employee Name", "Employee Code", "Department", "Designation", "Payroll Month"};
        String[] leftValues = {
                firstNonBlank(entry.getEmployeeDisplayName(), employeeCode),
                employeeCode,
                entry.getEmployeeDepartmentName(),
                entry.getEmployeeDesignationName(),
                run.getPayrollMonth().format(MONTH)
        };

        String[] rightLabels = {"Date of Joining", "PAN", "UAN", "ESI", "Bank Account"};
        String[] rightValues = {
                entry.getEmployeeDateOfJoining() == null ? "" : entry.getEmployeeDateOfJoining().format(DATE),
                entry.getEmployeePanDisplay(),
                entry.getEmployeeUanDisplay(),
                entry.getEmployeeEsiDisplay(),
                entry.getEmployeeBankAccountDisplay()
        };

        for (int row = 0; row < 5; row++) {
            float y = top - row * rowHeight - 16f;
            keyValue(stream, regular, bold, LEFT + 8f, y, 78f, leftLabels[row], leftValues[row]);
            keyValue(stream, regular, bold, MID + 8f, y, 76f, rightLabels[row], rightValues[row]);
        }
    }

    private void drawPayTable(
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            PayrollEntry entry) throws IOException {
        float top = 534f;
        float bottom = 384f;
        float headerHeight = 26f;
        float rowHeight = (top - bottom - headerHeight) / 5f;
        float leftAmountX = MID - 10f;
        float rightAmountX = RIGHT - 10f;

        rectangle(stream, LEFT, bottom, CONTENT_WIDTH, top - bottom);
        line(stream, MID, bottom, MID, top);
        line(stream, LEFT, top - headerHeight, RIGHT, top - headerHeight);
        for (int row = 1; row < 5; row++) {
            float y = top - headerHeight - row * rowHeight;
            line(stream, LEFT, y, RIGHT, y);
        }

        text(stream, bold, 10.5f, LEFT + 8f, top - 17f, "EARNINGS");
        rightText(stream, bold, 10.5f, leftAmountX, top - 17f, "AMOUNT");
        text(stream, bold, 10.5f, MID + 8f, top - 17f, "DEDUCTIONS");
        rightText(stream, bold, 10.5f, rightAmountX, top - 17f, "AMOUNT");

        String[] earningLabels = {"Basic Pay", "Allowances", "", "", "Gross Earnings"};
        BigDecimal[] earningValues = {
                entry.getBasicPay(),
                entry.getAllowanceTotal(),
                null,
                null,
                entry.getGrossPay()
        };

        String[] deductionLabels = {
                "Configured / Other",
                "Employee PF",
                "Employee ESI",
                "TDS",
                "Total Deductions"
        };
        BigDecimal[] deductionValues = {
                entry.getConfiguredDeductionTotal(),
                entry.getEmployeePfContribution(),
                entry.getEmployeeEsiContribution(),
                entry.getTdsAmount(),
                entry.getDeductionTotal()
        };

        for (int row = 0; row < 5; row++) {
            float y = top - headerHeight - row * rowHeight - 16f;
            PDFont rowFont = row == 4 ? bold : regular;
            if (hasText(earningLabels[row])) {
                text(stream, rowFont, 9.5f, LEFT + 8f, y, earningLabels[row]);
                rightText(stream, rowFont, 9.5f, leftAmountX, y, money(entry, earningValues[row]));
            }
            text(stream, rowFont, 9.5f, MID + 8f, y, deductionLabels[row]);
            rightText(stream, rowFont, 9.5f, rightAmountX, y, money(entry, deductionValues[row]));
        }
    }

    private void drawNetPay(
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            PayrollEntry entry) throws IOException {
        float top = 366f;
        float bottom = 306f;
        rectangle(stream, LEFT, bottom, CONTENT_WIDTH, top - bottom);

        text(stream, bold, 12f, LEFT + 12f, 345f, "NET PAYABLE");
        rightText(stream, bold, 17f, RIGHT - 12f, 343f, money(entry, entry.getNetPay()));

        String words = amountInWordsFormatter.format(
                entry.getNetPay(),
                currency(entry));
        text(stream, regular, 9f, LEFT + 12f, 322f, "Amount in words: " + words);
    }

    private void drawStatutorySummary(
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            PayrollEntry entry) throws IOException {
        text(stream, bold, 11f, LEFT, 286f, "STATUTORY & EMPLOYER SUMMARY");

        float top = 274f;
        float bottom = 178f;
        float rowHeight = (top - bottom) / 4f;
        rectangle(stream, LEFT, bottom, CONTENT_WIDTH, top - bottom);
        line(stream, MID, bottom, MID, top);
        for (int row = 1; row < 4; row++) {
            float y = top - row * rowHeight;
            line(stream, LEFT, y, RIGHT, y);
        }

        String[] leftLabels = {"PF Contribution Wage", "ESI Contribution Wage", "Policy Version", "Tax Regime"};
        String[] leftValues = {
                money(entry, entry.getPfContributionWage()),
                money(entry, entry.getEsiContributionWage()),
                entry.getStatutoryPolicyVersion(),
                entry.getTaxRegime() == null ? "" : entry.getTaxRegime().name()
        };

        String[] rightLabels = {"Employer PF", "Employer ESI", "Employer Statutory Total", "Branding Revision"};
        String[] rightValues = {
                money(entry, entry.getEmployerPfContribution()),
                money(entry, entry.getEmployerEsiContribution()),
                money(entry, entry.getEmployerStatutoryContributionTotal()),
                entry.getOrganizationLogoAssetVersion() == null
                        ? "No logo snapshot"
                        : "Logo v" + entry.getOrganizationLogoAssetVersion()
        };

        for (int row = 0; row < 4; row++) {
            float y = top - row * rowHeight - 16f;
            keyValue(stream, regular, bold, LEFT + 8f, y, 108f, leftLabels[row], leftValues[row]);
            keyValue(stream, regular, bold, MID + 8f, y, 108f, rightLabels[row], rightValues[row]);
        }
    }

    private void drawFooter(
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            PayrollEntry entry,
            PayrollRun run) throws IOException {
        line(stream, LEFT, 157f, RIGHT, 157f);

        text(stream, regular, 8f, LEFT, 143f,
                "Payslip Ref: " + entry.getId() + " | Finalized: " + DATE_TIME.format(run.getFinalizedAt()));
        text(stream, regular, 8f, LEFT, 130f,
                "This is a system-generated payslip rendered from the finalized payroll snapshot; no signature is required.");

        if (hasText(entry.getAuthorizedSignatoryLabelSnapshot())) {
            rightText(stream, bold, 8.5f, RIGHT, 113f, entry.getAuthorizedSignatoryLabelSnapshot());
        }

        if (hasText(entry.getPayslipFooterTextSnapshot())) {
            wrappedText(
                    stream,
                    regular,
                    8f,
                    LEFT,
                    102f,
                    CONTENT_WIDTH,
                    10f,
                    entry.getPayslipFooterTextSnapshot(),
                    3);
        }
    }

    private void keyValue(
            PDPageContentStream stream,
            PDType1Font regular,
            PDType1Font bold,
            float x,
            float y,
            float labelWidth,
            String label,
            String value) throws IOException {
        text(stream, bold, 8.5f, x, y, label + ":");
        text(stream, regular, 8.5f, x + labelWidth, y, firstNonBlank(value, "-"));
    }

    private String money(PayrollEntry entry, BigDecimal amount) {
        return currency(entry) + " " + formatAmount(amount);
    }

    private String currency(PayrollEntry entry) {
        return firstNonBlank(entry.getOrganizationDefaultCurrency(), "INR").toUpperCase(Locale.ROOT);
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        synchronized (MONEY) {
            return MONEY.format(safeAmount.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private String snapshotEmployeeCode(PayrollEntry entry) {
        if (!hasText(entry.getEmployeeCodeSnapshot())) {
            return null;
        }
        return entry.getEmployeeCodeSnapshot().trim();
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

    private void rectangle(
            PDPageContentStream stream,
            float x,
            float y,
            float width,
            float height) throws IOException {
        stream.addRect(x, y, width, height);
        stream.setLineWidth(0.7f);
        stream.stroke();
    }

    private void line(
            PDPageContentStream stream,
            float x1,
            float y1,
            float x2,
            float y2) throws IOException {
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.setLineWidth(0.5f);
        stream.stroke();
    }

    private void centeredText(
            PDPageContentStream stream,
            PDFont font,
            float size,
            float left,
            float right,
            float y,
            String value) throws IOException {
        String safe = sanitizePdfText(value);
        float width = stringWidth(font, size, safe);
        text(stream, font, size, left + ((right - left) - width) / 2f, y, safe);
    }

    private void rightText(
            PDPageContentStream stream,
            PDFont font,
            float size,
            float right,
            float y,
            String value) throws IOException {
        String safe = sanitizePdfText(value);
        text(stream, font, size, right - stringWidth(font, size, safe), y, safe);
    }

    private float wrappedText(
            PDPageContentStream stream,
            PDFont font,
            float size,
            float x,
            float y,
            float maxWidth,
            float lineHeight,
            String value,
            int maxLines) throws IOException {
        List<String> lines = wrap(font, size, value, maxWidth, maxLines);
        float currentY = y;
        for (String line : lines) {
            text(stream, font, size, x, currentY, line);
            currentY -= lineHeight;
        }
        return currentY;
    }

    private List<String> wrap(
            PDFont font,
            float size,
            String value,
            float maxWidth,
            int maxLines) throws IOException {
        String safe = sanitizePdfText(value).replaceAll("\\s+", " ").trim();
        if (safe.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : safe.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (stringWidth(font, size, candidate) <= maxWidth || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            lines.add(current.toString());
            if (lines.size() == maxLines) {
                return lines;
            }
            current.setLength(0);
            current.append(word);
        }

        if (!current.isEmpty() && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        return lines;
    }

    private float stringWidth(PDFont font, float size, String value) throws IOException {
        return font.getStringWidth(sanitizePdfText(value)) / 1000f * size;
    }

    private void text(
            PDPageContentStream stream,
            PDFont font,
            float size,
            float x,
            float y,
            String value) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitizePdfText(value));
        stream.endText();
    }

    private String joinNonBlank(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(separator, parts);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
