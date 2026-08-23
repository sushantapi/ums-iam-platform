package com.ums.org.service;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ums.org.config.TrustedGatewayAuthenticationFilter;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationMember;
import com.ums.org.entity.OrganizationSecurityPolicy;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationSecurityPolicyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

	private final OrganizationMemberRepository memberRepository;
	private final OrganizationSecurityPolicyRepository securityPolicyRepository;

	public void assertCanViewOrganization(UUID actorId, Organization organization, boolean superAdmin) {
		assertOrganizationActive(organization);
		assertMfaPolicySatisfied(organization, superAdmin);

		if (superAdmin || organization.getOwnerId().equals(actorId)) {
			return;
		}

		OrganizationMember membership = findMembership(organization, actorId,
				"User is not a member of this organization");

		if (membership.getRole() == OrganizationRole.OWNER
				|| membership.getRole() == OrganizationRole.ADMIN
				|| membership.getRole() == OrganizationRole.MEMBER) {
			return;
		}

		throw new AccessDeniedException("User is not allowed to access this organization");
	}

	public void assertCanManageMembers(UUID actorId, Organization organization, boolean superAdmin) {
		assertOrganizationActive(organization);
		assertMfaPolicySatisfied(organization, superAdmin);

		if (superAdmin || organization.getOwnerId().equals(actorId)) {
			return;
		}

		OrganizationMember membership = findMembership(organization, actorId, "User is not an organization admin");

		if (membership.getRole() == OrganizationRole.OWNER || membership.getRole() == OrganizationRole.ADMIN) {
			return;
		}

		throw new AccessDeniedException("User is not allowed to manage this organization");
	}

	private void assertMfaPolicySatisfied(Organization organization, boolean superAdmin) {
		if (superAdmin) {
			return;
		}

		boolean requireMfa = securityPolicyRepository.findById(organization.getId())
				.map(OrganizationSecurityPolicy::isRequireMfa)
				.orElse(false);
		if (!requireMfa) {
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String requiredOrganizationAuthority =
				TrustedGatewayAuthenticationFilter.ORGANIZATION_CONTEXT_AUTHORITY_PREFIX + organization.getId();
		boolean matchingOrganizationContext = hasAuthority(authentication, requiredOrganizationAuthority);
		boolean mfaVerified = hasAuthority(authentication, TrustedGatewayAuthenticationFilter.MFA_VERIFIED_AUTHORITY);

		if (!matchingOrganizationContext || !mfaVerified) {
			throw new AccessDeniedException("Organization requires an MFA-verified organization session");
		}
	}

	private boolean hasAuthority(Authentication authentication, String requiredAuthority) {
		return authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getAuthorities().stream()
						.anyMatch(authority -> requiredAuthority.equals(authority.getAuthority()));
	}

	private OrganizationMember findMembership(Organization organization, UUID actorId, String errorMessage) {
		return memberRepository.findByOrganizationIdAndUserId(organization.getId(), actorId)
				.orElseThrow(() -> new AccessDeniedException(errorMessage));
	}

	private void assertOrganizationActive(Organization organization) {
		if (organization.getStatus() != OrganizationStatus.ACTIVE) {
			throw new BadRequestException("Organization is not active");
		}
	}
}
