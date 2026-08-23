package com.ums.org.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ums.org.config.TrustedGatewayAuthenticationFilter;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationSecurityPolicy;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationSecurityPolicyRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTests {

	@Mock
	private OrganizationMemberRepository memberRepository;

	@Mock
	private OrganizationSecurityPolicyRepository securityPolicyRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void policyDisabledPreservesOwnerAccessWithoutOrganizationSessionContext() {
		UUID organizationId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, ownerId);
		when(securityPolicyRepository.findById(organizationId)).thenReturn(Optional.empty());

		OrganizationAccessService service = service();

		assertThatCode(() -> service.assertCanManageMembers(ownerId, organization, false))
				.doesNotThrowAnyException();
	}

	@Test
	void policyRequiredRejectsPlatformOnlyOwnerSession() {
		UUID organizationId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, ownerId);
		when(securityPolicyRepository.findById(organizationId)).thenReturn(Optional.of(requiredPolicy(organizationId)));
		authenticate(ownerId, List.of());

		assertThatThrownBy(() -> service().assertCanViewOrganization(ownerId, organization, false))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("MFA-verified organization session");
	}

	@Test
	void policyRequiredRejectsWrongOrganizationOrMissingMfaAssurance() {
		UUID organizationId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, ownerId);
		when(securityPolicyRepository.findById(organizationId)).thenReturn(Optional.of(requiredPolicy(organizationId)));

		authenticate(ownerId, List.of(
				new SimpleGrantedAuthority(
						TrustedGatewayAuthenticationFilter.ORGANIZATION_CONTEXT_AUTHORITY_PREFIX + UUID.randomUUID()),
				new SimpleGrantedAuthority(TrustedGatewayAuthenticationFilter.MFA_VERIFIED_AUTHORITY)));
		assertThatThrownBy(() -> service().assertCanViewOrganization(ownerId, organization, false))
				.isInstanceOf(AccessDeniedException.class);

		authenticate(ownerId, List.of(
				new SimpleGrantedAuthority(
						TrustedGatewayAuthenticationFilter.ORGANIZATION_CONTEXT_AUTHORITY_PREFIX + organizationId)));
		assertThatThrownBy(() -> service().assertCanViewOrganization(ownerId, organization, false))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void policyRequiredAllowsMatchingMfaVerifiedOrganizationSession() {
		UUID organizationId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, ownerId);
		when(securityPolicyRepository.findById(organizationId)).thenReturn(Optional.of(requiredPolicy(organizationId)));
		authenticate(ownerId, List.of(
				new SimpleGrantedAuthority(
						TrustedGatewayAuthenticationFilter.ORGANIZATION_CONTEXT_AUTHORITY_PREFIX + organizationId),
				new SimpleGrantedAuthority(TrustedGatewayAuthenticationFilter.MFA_VERIFIED_AUTHORITY)));

		assertThatCode(() -> service().assertCanManageMembers(ownerId, organization, false))
				.doesNotThrowAnyException();
	}

	@Test
	void superAdminBypassesOrganizationMfaSessionContextRequirement() {
		UUID organizationId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, UUID.randomUUID());

		assertThatCode(() -> service().assertCanManageMembers(actorId, organization, true))
				.doesNotThrowAnyException();
	}

	private OrganizationAccessService service() {
		return new OrganizationAccessService(memberRepository, securityPolicyRepository);
	}

	private void authenticate(UUID actorId, List<SimpleGrantedAuthority> authorities) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(actorId.toString(), null, authorities));
	}

	private Organization activeOrganization(UUID organizationId, UUID ownerId) {
		return Organization.builder()
				.id(organizationId)
				.name("Security Boundary Test")
				.slug("security-boundary-test")
				.ownerId(ownerId)
				.status(OrganizationStatus.ACTIVE)
				.build();
	}

	private OrganizationSecurityPolicy requiredPolicy(UUID organizationId) {
		return OrganizationSecurityPolicy.builder()
				.organizationId(organizationId)
				.requireMfa(true)
				.build();
	}
}
