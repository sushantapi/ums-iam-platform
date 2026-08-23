package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreateSalaryStructureRequest;
import com.ums.hrms.payroll.dto.SalaryStructureResponse;
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

        SalaryStructure structure = new SalaryStructure();
        structure.setOrganizationId(request.organizationId());
        structure.setEmployeeId(request.employeeId());
        structure.setCurrency(normalizeCurrency(request.currency()));
        structure.setBasicPay(request.basicPay());
        structure.setAllowanceTotal(request.allowanceTotal());
        structure.setDeductionTotal(request.deductionTotal());
        structure.setPfApplicable(Boolean.TRUE.equals(request.pfApplicable()));
        structure.setPfContributionWage(request.pfContributionWage());
        structure.setEsiApplicable(Boolean.TRUE.equals(request.esiApplicable()));
        structure.setEsiContributionWage(request.esiContributionWage());
        structure.setTdsAmount(request.tdsAmount() == null ? BigDecimal.ZERO : request.tdsAmount());
        structure.setTaxRegime(request.taxRegime());
        structure.setEffectiveFrom(request.effectiveFrom());
        structure.setEffectiveTo(request.effectiveTo());
        structure.setActive(request.active() == null || request.active());
        structure.setCreatedBy(actorUserId);

        SalaryStructure saved = salaryStructureRepository.save(structure);
        payrollAuditPublisher.publishSalaryStructureCreated(saved, actorUserId);
        return toResponse(saved);
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
        validateMoney("basicPay", request.basicPay());
        validateMoney("allowanceTotal", request.allowanceTotal());
        validateMoney("deductionTotal", request.deductionTotal());
        validateOptionalMoney("pfContributionWage", request.pfContributionWage());
        validateOptionalMoney("esiContributionWage", request.esiContributionWage());
        validateOptionalMoney("tdsAmount", request.tdsAmount());

        if (Boolean.TRUE.equals(request.pfApplicable()) && request.pfContributionWage() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "pfContributionWage is required when PF is applicable");
        }

        if (Boolean.TRUE.equals(request.esiApplicable()) && request.esiContributionWage() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "esiContributionWage is required when ESI is applicable");
        }

        if (request.effectiveFrom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "effectiveFrom is required");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "effectiveTo cannot be before effectiveFrom");
        }

        String currency = normalizeCurrency(request.currency());
        if (!currency.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency must be a 3-letter ISO code");
        }
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
                structure.getCreatedBy(),
                structure.getCreatedAt(),
                structure.getUpdatedAt());
    }
}
