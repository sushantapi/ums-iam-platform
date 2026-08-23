package com.ums.org.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.dto.OrganizationSecurityPolicyInternalResponse;
import com.ums.org.dto.OrganizationSecurityPolicyResponse;
import com.ums.org.dto.UpdateOrganizationSecurityPolicyRequest;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationSecurityPolicy;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.repositoty.OrganizationSecurityPolicyRepository;
import com.ums.org.service.OrganizationAccessService;
import com.ums.org.service.OrganizationSecurityPolicyService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationSecurityPolicyServiceImpl implements OrganizationSecurityPolicyService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationSecurityPolicyRepository policyRepository;
	private final OrganizationAccessService accessService;
	private final AuditPublisher auditPublisher;

	@Override
	public OrganizationSecurityPolicyResponse getPolicy(UUID organizationId, UUID actorUserId, boolean superAdmin) {
		Organization organization = findOrganization(organizationId);
		accessService.assertCanViewOrganization(actorUserId, organization, superAdmin);
		return toResponse(organizationId, policyRepository.findById(organizationId).orElse(null));
	}

	@Override
	public OrganizationSecurityPolicyResponse updatePolicy(UUID organizationId,
			UpdateOrganizationSecurityPolicyRequest request,
			UUID actorUserId,
			boolean superAdmin) {
		Organization organization = findOrganization(organizationId);
		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

		OrganizationSecurityPolicy policy = policyRepository.findById(organizationId)
				.orElseGet(() -> OrganizationSecurityPolicy.builder()
						.organizationId(organizationId)
						.requireMfa(false)
						.build());

		boolean previousRequireMfa = policy.isRequireMfa();
		policy.setRequireMfa(request.requireMfa());
		policy.setUpdatedBy(actorUserId);
		OrganizationSecurityPolicy saved = policyRepository.save(policy);

		publishAuditSafely(AuditEvent.builder()
				.eventType("organization.security.policy.updated")
				.serviceName("organization-service")
				.userId(actorUserId.toString())
				.action("ORGANIZATION_SECURITY_POLICY_UPDATE")
				.entityType("ORGANIZATION_SECURITY_POLICY")
				.entityId(organizationId.toString())
				.details("requireMfa changed from " + previousRequireMfa + " to " + saved.isRequireMfa())
				.timestamp(LocalDateTime.now())
				.build());

		return toResponse(organizationId, saved);
	}

	@Override
	public OrganizationSecurityPolicyInternalResponse getInternalPolicy(UUID organizationId) {
		Organization organization = findOrganization(organizationId);
		boolean requireMfa = policyRepository.findById(organizationId)
				.map(OrganizationSecurityPolicy::isRequireMfa)
				.orElse(false);
		return new OrganizationSecurityPolicyInternalResponse(
				organizationId,
				requireMfa,
				organization.getStatus() == OrganizationStatus.ACTIVE);
	}

	private Organization findOrganization(UUID organizationId) {
		return organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
	}

	private OrganizationSecurityPolicyResponse toResponse(UUID organizationId, OrganizationSecurityPolicy policy) {
		if (policy == null) {
			return new OrganizationSecurityPolicyResponse(organizationId, false, null);
		}
		return new OrganizationSecurityPolicyResponse(organizationId, policy.isRequireMfa(), policy.getUpdatedAt());
	}

	private void publishAuditSafely(AuditEvent event) {
		try {
			auditPublisher.publish(event);
		} catch (RuntimeException ex) {
			log.warn("Organization security-policy audit publication failed organizationId={} failureType={}",
					event.getEntityId(), ex.getClass().getName());
		}
	}
}
