package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreatePayrollRunRequest;
import com.ums.hrms.payroll.dto.PayrollEntryResponse;
import com.ums.hrms.payroll.dto.PayrollRunResponse;
import com.ums.hrms.payroll.dto.PayrollTransitionRequest;
import com.ums.hrms.payroll.entity.PayrollEntry;
import com.ums.hrms.payroll.entity.PayrollRun;
import com.ums.hrms.payroll.entity.PayrollRunStatus;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.StatutoryPolicy;
import com.ums.hrms.payroll.repository.PayrollEntryRepository;
import com.ums.hrms.payroll.repository.PayrollRunRepository;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;
import com.ums.hrms.payroll.repository.StatutoryPolicyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollRunService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollEntryRepository payrollEntryRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final StatutoryPolicyRepository statutoryPolicyRepository;
    private final OrganizationAccessService organizationAccessService;
    private final PayrollAuditPublisher payrollAuditPublisher;
    private final StatutoryPayrollCalculator statutoryPayrollCalculator;

    public PayrollRunResponse create(
            CreatePayrollRunRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);

        if (payrollRunRepository.existsByOrganizationIdAndPayrollMonth(
                request.organizationId(), request.payrollMonth())) {
            throw duplicateRun();
        }

        PayrollRun run = new PayrollRun();
        run.setOrganizationId(request.organizationId());
        run.setPayrollMonth(request.payrollMonth());
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setCreatedBy(actorUserId);

        try {
            return toRunResponse(payrollRunRepository.saveAndFlush(run));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateRun(ex);
        }
    }

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> list(
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return payrollRunRepository.findAllByOrganizationIdOrderByPayrollMonthDesc(organizationId)
                .stream()
                .map(this::toRunResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse get(
            UUID id,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return toRunResponse(findRun(id, organizationId));
    }

    public PayrollRunResponse process(
            UUID id,
            PayrollTransitionRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        UUID organizationId = request.organizationId();
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);

        PayrollRun run = payrollRunRepository.findByIdAndOrganizationIdForUpdate(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run not found"));
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT payroll runs can be processed");
        }

        LocalDate effectiveOn = run.getPayrollMonth().atEndOfMonth();
        List<SalaryStructure> structures = salaryStructureRepository
                .findAllActiveEffectiveOn(organizationId, effectiveOn);
        if (structures.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No active salary structures found for payroll month");
        }

        StatutoryPolicy statutoryPolicy = resolveStatutoryPolicy(
                organizationId,
                effectiveOn,
                structures);

        LocalDateTime generatedAt = LocalDateTime.now();
        List<PayrollEntry> entries = structures.stream()
                .map(structure -> createEntry(
                        run,
                        structure,
                        statutoryPolicy,
                        generatedAt))
                .toList();
        payrollEntryRepository.saveAll(entries);

        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedBy(actorUserId);
        run.setProcessedAt(LocalDateTime.now());
        PayrollRun saved = payrollRunRepository.save(run);
        payrollAuditPublisher.publishPayrollRunProcessed(saved, actorUserId, entries.size());
        return toRunResponse(saved);
    }

    public PayrollRunResponse finalizeRun(
            UUID id,
            PayrollTransitionRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        UUID organizationId = request.organizationId();
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);

        PayrollRun run = payrollRunRepository.findByIdAndOrganizationIdForUpdate(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run not found"));
        if (run.getStatus() != PayrollRunStatus.PROCESSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only PROCESSED payroll runs can be finalized");
        }

        run.setStatus(PayrollRunStatus.FINALIZED);
        run.setFinalizedBy(actorUserId);
        run.setFinalizedAt(LocalDateTime.now());
        PayrollRun saved = payrollRunRepository.save(run);
        payrollAuditPublisher.publishPayrollRunFinalized(saved, actorUserId);
        return toRunResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PayrollEntryResponse> listEntries(
            UUID runId,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        findRun(runId, organizationId);
        return payrollEntryRepository
                .findAllByPayrollRunIdAndOrganizationIdOrderByEmployeeId(runId, organizationId)
                .stream()
                .map(this::toEntryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PayrollEntryResponse getPayslip(
            UUID entryId,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return payrollEntryRepository.findByIdAndOrganizationId(entryId, organizationId)
                .map(this::toEntryResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll entry not found"));
    }

    private PayrollRun findRun(UUID id, UUID organizationId) {
        return payrollRunRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run not found"));
    }

    private PayrollEntry createEntry(
            PayrollRun run,
            SalaryStructure structure,
            StatutoryPolicy statutoryPolicy,
            LocalDateTime generatedAt) {
        BigDecimal basicPay = money(structure.getBasicPay());
        BigDecimal allowanceTotal = money(structure.getAllowanceTotal());
        BigDecimal configuredDeductionTotal = money(structure.getDeductionTotal());
        BigDecimal grossPay = money(basicPay.add(allowanceTotal));

        StatutoryPayrollCalculation statutoryCalculation =
                statutoryPayrollCalculator.calculate(structure, statutoryPolicy);

        BigDecimal deductionTotal = money(
                configuredDeductionTotal.add(
                        statutoryCalculation.statutoryEmployeeDeductionTotal()));
        BigDecimal netPay = money(grossPay.subtract(deductionTotal));

        if (netPay.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payroll net pay cannot be negative for employee " + structure.getEmployeeId());
        }

        PayrollEntry entry = new PayrollEntry();
        entry.setPayrollRunId(run.getId());
        entry.setOrganizationId(run.getOrganizationId());
        entry.setEmployeeId(structure.getEmployeeId());
        entry.setSalaryStructureId(structure.getId());
        entry.setBasicPay(basicPay);
        entry.setAllowanceTotal(allowanceTotal);
        entry.setGrossPay(grossPay);
        entry.setConfiguredDeductionTotal(configuredDeductionTotal);
        entry.setDeductionTotal(deductionTotal);
        entry.setNetPay(netPay);

        entry.setStatutoryPolicyId(statutoryCalculation.statutoryPolicyId());
        entry.setStatutoryPolicyVersion(statutoryCalculation.statutoryPolicyVersion());
        entry.setPfContributionWage(statutoryCalculation.pfContributionWage());
        entry.setEmployeePfContribution(statutoryCalculation.employeePfContribution());
        entry.setEmployerPfContribution(statutoryCalculation.employerPfContribution());
        entry.setEsiContributionWage(statutoryCalculation.esiContributionWage());
        entry.setEmployeeEsiContribution(statutoryCalculation.employeeEsiContribution());
        entry.setEmployerEsiContribution(statutoryCalculation.employerEsiContribution());
        entry.setTdsAmount(statutoryCalculation.tdsAmount());
        entry.setStatutoryEmployeeDeductionTotal(
                statutoryCalculation.statutoryEmployeeDeductionTotal());
        entry.setEmployerStatutoryContributionTotal(
                statutoryCalculation.employerStatutoryContributionTotal());
        entry.setTaxRegime(statutoryCalculation.taxRegime());

        entry.setGeneratedAt(generatedAt);
        return entry;
    }

    private StatutoryPolicy resolveStatutoryPolicy(
            UUID organizationId,
            LocalDate effectiveOn,
            List<SalaryStructure> structures) {
        boolean policyRequired = structures.stream()
                .anyMatch(structure ->
                        structure.isPfApplicable() || structure.isEsiApplicable());

        if (!policyRequired) {
            return null;
        }

        List<StatutoryPolicy> policies = statutoryPolicyRepository
                .findAllActiveEffectiveOn(
                        organizationId,
                        "IN",
                        effectiveOn);

        if (policies.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No active statutory policy found for payroll month");
        }

        if (policies.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple active statutory policies found for payroll month");
        }

        return policies.getFirst();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private ResponseStatusException duplicateRun() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payroll run already exists for organization and month");
    }

    private ResponseStatusException duplicateRun(DataIntegrityViolationException cause) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payroll run already exists for organization and month",
                cause);
    }

    private PayrollRunResponse toRunResponse(PayrollRun run) {
        return new PayrollRunResponse(
                run.getId(),
                run.getOrganizationId(),
                run.getPayrollMonth(),
                run.getStatus(),
                run.getCreatedBy(),
                run.getProcessedBy(),
                run.getProcessedAt(),
                run.getFinalizedBy(),
                run.getFinalizedAt(),
                run.getCreatedAt(),
                run.getUpdatedAt());
    }

    private PayrollEntryResponse toEntryResponse(PayrollEntry entry) {
        return new PayrollEntryResponse(
                entry.getId(),
                entry.getPayrollRunId(),
                entry.getOrganizationId(),
                entry.getEmployeeId(),
                entry.getSalaryStructureId(),
                entry.getBasicPay(),
                entry.getAllowanceTotal(),
                entry.getGrossPay(),
                entry.getConfiguredDeductionTotal(),
                entry.getPfContributionWage(),
                entry.getEmployeePfContribution(),
                entry.getEmployerPfContribution(),
                entry.getEsiContributionWage(),
                entry.getEmployeeEsiContribution(),
                entry.getEmployerEsiContribution(),
                entry.getTdsAmount(),
                entry.getStatutoryEmployeeDeductionTotal(),
                entry.getEmployerStatutoryContributionTotal(),
                entry.getStatutoryPolicyId(),
                entry.getStatutoryPolicyVersion(),
                entry.getTaxRegime(),
                entry.getDeductionTotal(),
                entry.getNetPay(),
                entry.getGeneratedAt());
    }
}
