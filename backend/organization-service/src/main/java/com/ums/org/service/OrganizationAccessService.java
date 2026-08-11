package com.ums.org.service;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationMember;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.repositoty.OrganizationMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

	private final OrganizationMemberRepository memberRepository;

	public void assertCanViewOrganization(UUID actorId, Organization organization, boolean superAdmin) {
		assertOrganizationActive(organization);

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

	public void assertCanManageMembers(UUID actorId, Organization organization) {
		assertOrganizationActive(organization);

		if (organization.getOwnerId().equals(actorId)) {
			return;
		}

		OrganizationMember membership = findMembership(organization, actorId, "User is not an organization admin");

		if (membership.getRole() == OrganizationRole.OWNER || membership.getRole() == OrganizationRole.ADMIN) {
			return;
		}

		throw new AccessDeniedException("User is not allowed to manage this organization");
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
