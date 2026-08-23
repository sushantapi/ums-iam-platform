package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.payroll.dto.CreateStatutoryPolicyRequest;
import com.ums.hrms.payroll.dto.StatutoryPolicyResponse;
import com.ums.hrms.payroll.entity.StatutoryPolicy;
import com.ums.hrms.payroll.repository.StatutoryPolicyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StatutoryPolicyService {

    private static final String SUPPORTED_COUNTRY = "IN";

    private final StatutoryPolicyRepository statutoryPolicyRepository;
    private final OrganizationAccessService organizationAccessService;
    private final PayrollAuditPublisher payrollAuditPublisher;

    public StatutoryPolicyResponse create(
            CreateStatutoryPolicyRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(
                request.organizationId(),
                actorUserId,
                superAdmin);

        validateRequest(request);

        String countryCode = normalizeCountryCode(request.countryCode());
        String policyVersion = request.policyVersion().trim();
        boolean active = request.active() == null || request.active();

        if (statutoryPolicyRepository.existsByOrganizationIdAndCountryCodeAndPolicyVersion(
                request.organizationId(),
                countryCode,
                policyVersion)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Statutory policy version already exists");
        }

        if (active && statutoryPolicyRepository.countOverlappingActiveEffectiveRanges(
                request.organizationId(),
                countryCode,
                request.effectiveFrom(),
                request.effectiveTo()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Overlapping active statutory policy effective range exists");
        }

        StatutoryPolicy policy = new StatutoryPolicy();
        policy.setOrganizationId(request.organizationId());
        policy.setCountryCode(countryCode);
        policy.setPolicyVersion(policyVersion);
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setEffectiveTo(request.effectiveTo());
        policy.setActive(active);
        policy.setPfEmployeeRate(request.pfEmployeeRate());
        policy.setPfEmployerRate(request.pfEmployerRate());
        policy.setPfContributionWageCeiling(request.pfContributionWageCeiling());
        policy.setEsiEmployeeRate(request.esiEmployeeRate());
        policy.setEsiEmployerRate(request.esiEmployerRate());
        policy.setEsiWageEligibilityCeiling(request.esiWageEligibilityCeiling());
        policy.setCreatedBy(actorUserId);

        StatutoryPolicy saved = statutoryPolicyRepository.save(policy);
        payrollAuditPublisher.publishStatutoryPolicyCreated(saved, actorUserId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StatutoryPolicyResponse> list(
            UUID organizationId,
            String countryCode,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(
                organizationId,
                actorUserId,
                superAdmin);

        String normalizedCountryCode = normalizeCountryCode(countryCode);

        return statutoryPolicyRepository
                .findAllByOrganizationIdAndCountryCodeOrderByEffectiveFromDesc(
                        organizationId,
                        normalizedCountryCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatutoryPolicyResponse get(
            UUID id,
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        organizationAccessService.assertCanAccess(
                organizationId,
                actorUserId,
                superAdmin);

        return statutoryPolicyRepository
                .findByIdAndOrganizationId(id, organizationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Statutory policy not found"));
    }

    private void validateRequest(CreateStatutoryPolicyRequest request) {
        if (request.organizationId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "organizationId is required");
        }

        normalizeCountryCode(request.countryCode());

        if (request.policyVersion() == null || request.policyVersion().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "policyVersion is required");
        }

        if (request.policyVersion().trim().length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "policyVersion exceeds 50 characters");
        }

        if (request.effectiveFrom() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "effectiveFrom is required");
        }

        if (request.effectiveTo() != null
                && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "effectiveTo cannot be before effectiveFrom");
        }

        validateRate("pfEmployeeRate", request.pfEmployeeRate());
        validateRate("pfEmployerRate", request.pfEmployerRate());
        validateMoney(
                "pfContributionWageCeiling",
                request.pfContributionWageCeiling());

        validateRate("esiEmployeeRate", request.esiEmployeeRate());
        validateRate("esiEmployerRate", request.esiEmployerRate());
        validateMoney(
                "esiWageEligibilityCeiling",
                request.esiWageEligibilityCeiling());
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "countryCode is required");
        }

        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);

        if (!SUPPORTED_COUNTRY.equals(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "countryCode must be IN for the current statutory payroll foundation");
        }

        return normalized;
    }

    private void validateRate(String field, BigDecimal value) {
        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " is required");
        }

        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be between 0 and 1");
        }

        if (value.scale() > 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " supports at most 6 decimal places");
        }
    }

    private void validateMoney(String field, BigDecimal value) {
        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " is required");
        }

        if (value.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " cannot be negative");
        }

        if (value.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " supports at most 2 decimal places");
        }

        if (value.precision() - value.scale() > 17) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " exceeds supported precision");
        }
    }

    private StatutoryPolicyResponse toResponse(StatutoryPolicy policy) {
        return new StatutoryPolicyResponse(
                policy.getId(),
                policy.getOrganizationId(),
                policy.getCountryCode(),
                policy.getPolicyVersion(),
                policy.getEffectiveFrom(),
                policy.getEffectiveTo(),
                policy.isActive(),
                policy.getPfEmployeeRate(),
                policy.getPfEmployerRate(),
                policy.getPfContributionWageCeiling(),
                policy.getEsiEmployeeRate(),
                policy.getEsiEmployerRate(),
                policy.getEsiWageEligibilityCeiling(),
                policy.getCreatedBy(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }
}