package com.ums.org.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.dto.OrganizationProfileResponse;
import com.ums.org.dto.UpdateOrganizationProfileRequest;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationProfile;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.repositoty.OrganizationProfileRepository;
import com.ums.org.repositoty.OrganizationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationProfileService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationProfileRepository profileRepository;
    private final OrganizationAccessService accessService;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public OrganizationProfileResponse get(
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        Organization organization = getOrganization(organizationId);
        accessService.assertCanViewOrganization(actorUserId, organization, superAdmin);
        return resolveProfileResponse(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationProfileResponse getInternal(UUID organizationId) {
        Organization organization = getOrganization(organizationId);
        return resolveProfileResponse(organization);
    }

    @Transactional
    public OrganizationProfileResponse update(
            UUID organizationId,
            UpdateOrganizationProfileRequest request,
            UUID actorUserId,
            boolean superAdmin) {
        Organization organization = getOrganization(organizationId);
        accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

        OrganizationProfile profile = profileRepository.findById(organizationId)
                .orElseGet(() -> OrganizationProfile.builder()
                        .organizationId(organizationId)
                        .build());

        profile.setLegalName(normalizeNullable(request.legalName()));
        profile.setDisplayName(normalizeNullable(request.displayName()));
        profile.setRegisteredAddress(normalizeNullable(request.registeredAddress()));
        profile.setBusinessEmail(normalizeLowerCase(request.businessEmail()));
        profile.setBusinessPhone(normalizeNullable(request.businessPhone()));
        profile.setWebsite(normalizeNullable(request.website()));
        profile.setDefaultCurrency(normalizeUpperCase(request.defaultCurrency()));
        profile.setPayrollCountry(normalizeUpperCase(request.payrollCountry()));
        profile.setPayslipFooterText(normalizeNullable(request.payslipFooterText()));
        profile.setAuthorizedSignatoryLabel(normalizeNullable(request.authorizedSignatoryLabel()));

        OrganizationProfile saved = profileRepository.save(profile);
        publishProfileUpdatedAudit(actorUserId, organizationId);
        return toResponse(organization, saved);
    }

    private OrganizationProfileResponse resolveProfileResponse(Organization organization) {
        return profileRepository.findById(organization.getId())
                .map(profile -> toResponse(organization, profile))
                .orElseGet(() -> fallbackResponse(organization));
    }

    private Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private OrganizationProfileResponse fallbackResponse(Organization organization) {
        return new OrganizationProfileResponse(
                organization.getId(),
                organization.getName(),
                organization.getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private OrganizationProfileResponse toResponse(
            Organization organization,
            OrganizationProfile profile) {
        return new OrganizationProfileResponse(
                organization.getId(),
                fallback(profile.getLegalName(), organization.getName()),
                fallback(profile.getDisplayName(), organization.getName()),
                profile.getRegisteredAddress(),
                profile.getBusinessEmail(),
                profile.getBusinessPhone(),
                profile.getWebsite(),
                profile.getDefaultCurrency(),
                profile.getPayrollCountry(),
                profile.getPayslipFooterText(),
                profile.getAuthorizedSignatoryLabel(),
                profile.getLogoAssetId(),
                profile.getLogoAssetVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeLowerCase(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpperCase(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private void publishProfileUpdatedAudit(UUID actorUserId, UUID organizationId) {
        try {
            auditPublisher.publish(AuditEvent.builder()
                    .eventType("organization.profile.updated")
                    .serviceName("organization-service")
                    .userId(actorUserId.toString())
                    .action("ORGANIZATION_PROFILE_UPDATE")
                    .entityType("ORGANIZATION_PROFILE")
                    .entityId(organizationId.toString())
                    .details("Organization profile updated")
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn(
                    "Organization profile audit publication failed organizationId={} failureType={}",
                    organizationId,
                    ex.getClass().getName());
        }
    }
}
