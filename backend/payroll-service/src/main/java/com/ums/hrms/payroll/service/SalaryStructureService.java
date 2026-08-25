package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreateSalaryStructureRequest;
import com.ums.hrms.payroll.dto.SalaryStructureResponse;
import com.ums.hrms.payroll.dto.SupersedeSalaryStructureRequest;
import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.repository.SalaryStructureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final OrganizationAccessService organizationAccessService;
    private final PayrollTenantValidationService employeeValidationService;
    private final PayrollAuditPublisher payrollAuditPublisher;

    public SalaryStructureResponse create(
            CreateSalaryStructureRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);
        employeeValidationService.validateActiveEmployee(request.employeeId(), request.organizationId());
        validateRequest(request);

        if (salaryStructureRepository.countOverlappingEffectiveRanges(
                request.organizationId(),
                request.employeeId(),
                request.effectiveFrom(),
                request.effectiveTo()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Overlapping salary structure effective range exists");
        }

        Optional<SalaryStructure> latest = salaryStructureRepository
                .findFirstByOrganizationIdAndEmployeeIdOrderByVersionNumberDesc(
                        request.organizationId(),
                        request.employeeId());

        if (latest.isPresent()
                && !request.effectiveFrom().isAfter(latest.get().getEffectiveFrom())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "New salary structure must start after the latest salary version");
        }

        SalaryStructure structure = new SalaryStructure();
        structure.setOrganizationId(request.organizationId());
        structure.setEmployeeId(request.employeeId());
        structure.setVersionNumber(latest.map(value -> value.getVersionNumber() + 1).orElse(1));
        structure.setSupersedesStructureId(latest.map(SalaryStructure::getId).orElse(null));
        applySalaryValues(
                structure,
                request.currency(),
                request.basicPay(),
                request.allowanceTotal(),
                request.deductionTotal(),
                request.pfApplicable(),
                request.pfContributionWage(),
                request.esiApplicable(),
                request.esiContributionWage(),
                request.tdsAmount(),
                request.taxRegime());
        structure.setEffectiveFrom(request.effectiveFrom());
        structure.setEffectiveTo(request.effectiveTo());
        structure.setActive(request.active() == null || request.active());
        structure.setCreatedBy(actorUserId);

        SalaryStructure saved = salaryStructureRepository.save(structure);
        payrollAuditPublisher.publishSalaryStructureCreated(saved, actorUserId);
        return toResponse(saved);
    }

    public SalaryStructureResponse supersede(
            UUID predecessorId,
            SupersedeSalaryStructureRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(request.organizationId(), actorUserId, superAdmin);

        SalaryStructure predecessor = salaryStructureRepository
                .findByIdAndOrganizationIdForUpdate(predecessorId, request.organizationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Salary structure not found"));

        employeeValidationService.validateActiveEmployee(
                predecessor.getEmployeeId(),
                predecessor.getOrganizationId());
        validateSupersedeRequest(request);

        if (!predecessor.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive salary structure cannot be superseded");
        }

        if (salaryStructureRepository.existsBySupersedesStructureId(predecessor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Salary structure has already been superseded");
        }

        LocalDate successorEffectiveFrom = request.effectiveFrom();
        if (!successorEffectiveFrom.isAfter(predecessor.getEffectiveFrom())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Successor effectiveFrom must be after predecessor effectiveFrom");
        }

        LocalDate predecessorOriginalEffectiveTo = predecessor.getEffectiveTo();
        if (predecessorOriginalEffectiveTo != null
                && successorEffectiveFrom.isAfter(predecessorOriginalEffectiveTo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Successor effectiveFrom must fall within the predecessor effective range");
        }

        if (salaryStructureRepository.countOverlappingEffectiveRangesExcludingId(
                predecessor.getOrganizationId(),
                predecessor.getEmployeeId(),
                predecessor.getId(),
                successorEffectiveFrom,
                predecessorOriginalEffectiveTo) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Successor salary structure would overlap another salary version");
        }

        predecessor.setEffectiveTo(successorEffectiveFrom.minusDays(1));
        predecessor.setSupersededAt(LocalDateTime.now());
        predecessor.setSupersededBy(actorUserId);
        salaryStructureRepository.save(predecessor);

        SalaryStructure successor = new SalaryStructure();
        successor.setOrganizationId(predecessor.getOrganizationId());
        successor.setEmployeeId(predecessor.getEmployeeId());
        successor.setVersionNumber(predecessor.getVersionNumber() + 1);
        successor.setSupersedesStructureId(predecessor.getId());
        applySalaryValues(
                successor,
                request.currency(),
                request.basicPay(),
                request.allowanceTotal(),
                request.deductionTotal(),
                request.pfApplicable(),
                request.pfContributionWage(),
                request.esiApplicable(),
                request.esiContributionWage(),
                request.tdsAmount(),
                request.taxRegime());
        successor.setEffectiveFrom(successorEffectiveFrom);
        successor.setEffectiveTo(predecessorOriginalEffectiveTo);
        successor.setActive(true);
        successor.setCreatedBy(actorUserId);

        try {
            SalaryStructure saved = salaryStructureRepository.saveAndFlush(successor);
            payrollAuditPublisher.publishSalaryStructureSuperseded(predecessor, saved, actorUserId);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Salary structure was superseded concurrently or violates version integrity",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> list(
            UUID organizationId,
            UUID employeeId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return salaryStructureRepository
                .findAllByOrganizationIdAndEmployeeIdOrderByEffectiveFromDesc(organizationId, employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalaryStructureResponse get(
            UUID id,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(organizationId, actorUserId, superAdmin);
        return salaryStructureRepository.findByIdAndOrganizationId(id, organizationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Salary structure not found"));
    }

    private void validateRequest(CreateSalaryStructureRequest request) {
        validateSalaryValues(
                request.currency(),
                request.basicPay(),
                request.allowanceTotal(),
                request.deductionTotal(),
                request.pfApplicable(),
                request.pfContributionWage(),
                request.esiApplicable(),
                request.esiContributionWage(),
                request.tdsAmount());

        if (request.effectiveFrom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "effectiveFrom is required");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "effectiveTo cannot be before effectiveFrom");
        }
    }

    private void validateSupersedeRequest(SupersedeSalaryStructureRequest request) {
        validateSalaryValues(
                request.currency(),
                request.basicPay(),
                request.allowanceTotal(),
                request.deductionTotal(),
                request.pfApplicable(),
                request.pfContributionWage(),
                request.esiApplicable(),
                request.esiContributionWage(),
                request.tdsAmount());

        if (request.effectiveFrom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "effectiveFrom is required");
        }
    }

    private void validateSalaryValues(
            String currency,
            BigDecimal basicPay,
            BigDecimal allowanceTotal,
            BigDecimal deductionTotal,
            Boolean pfApplicable,
            BigDecimal pfContributionWage,
            Boolean esiApplicable,
            BigDecimal esiContributionWage,
            BigDecimal tdsAmount) {
        validateMoney("basicPay", basicPay);
        validateMoney("allowanceTotal", allowanceTotal);
        validateMoney("deductionTotal", deductionTotal);
        validateOptionalMoney("pfContributionWage", pfContributionWage);
        validateOptionalMoney("esiContributionWage", esiContributionWage);
        validateOptionalMoney("tdsAmount", tdsAmount);

        if (Boolean.TRUE.equals(pfApplicable) && pfContributionWage == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "pfContributionWage is required when PF is applicable");
        }

        if (Boolean.TRUE.equals(esiApplicable) && esiContributionWage == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "esiContributionWage is required when ESI is applicable");
        }

        String normalizedCurrency = normalizeCurrency(currency);
        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency must be a 3-letter ISO code");
        }
    }

    private void applySalaryValues(
            SalaryStructure structure,
            String currency,
            BigDecimal basicPay,
            BigDecimal allowanceTotal,
            BigDecimal deductionTotal,
            Boolean pfApplicable,
            BigDecimal pfContributionWage,
            Boolean esiApplicable,
            BigDecimal esiContributionWage,
            BigDecimal tdsAmount,
            com.ums.hrms.payroll.entity.TaxRegime taxRegime) {
        structure.setCurrency(normalizeCurrency(currency));
        structure.setBasicPay(basicPay);
        structure.setAllowanceTotal(allowanceTotal);
        structure.setDeductionTotal(deductionTotal);
        structure.setPfApplicable(Boolean.TRUE.equals(pfApplicable));
        structure.setPfContributionWage(pfContributionWage);
        structure.setEsiApplicable(Boolean.TRUE.equals(esiApplicable));
        structure.setEsiContributionWage(esiContributionWage);
        structure.setTdsAmount(tdsAmount == null ? BigDecimal.ZERO : tdsAmount);
        structure.setTaxRegime(taxRegime);
    }

    private void validateOptionalMoney(String field, BigDecimal value) {
        if (value != null) {
            validateMoney(field, value);
        }
    }

    private void validateMoney(String field, BigDecimal value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        if (value.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " cannot be negative");
        }
        if (value.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " supports at most 2 decimal places");
        }
        if (value.precision() - value.scale() > 17) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " exceeds supported precision");
        }
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? "INR" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private SalaryStructureResponse toResponse(SalaryStructure structure) {
        return new SalaryStructureResponse(
                structure.getId(),
                structure.getOrganizationId(),
                structure.getEmployeeId(),
                structure.getCurrency(),
                structure.getBasicPay(),
                structure.getAllowanceTotal(),
                structure.getDeductionTotal(),
                structure.isPfApplicable(),
                structure.getPfContributionWage(),
                structure.isEsiApplicable(),
                structure.getEsiContributionWage(),
                structure.getTdsAmount(),
                structure.getTaxRegime(),
                structure.getEffectiveFrom(),
                structure.getEffectiveTo(),
                structure.isActive(),
                structure.getVersionNumber(),
                structure.getSupersedesStructureId(),
                structure.getSupersededAt(),
                structure.getSupersededBy(),
                structure.getCreatedBy(),
                structure.getCreatedAt(),
                structure.getUpdatedAt());
    }
}
