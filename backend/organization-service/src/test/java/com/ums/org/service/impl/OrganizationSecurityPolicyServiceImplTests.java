package com.ums.org.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.ums.events.publisher.AuditPublisher;
import com.ums.org.dto.UpdateOrganizationSecurityPolicyRequest;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationSecurityPolicy;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.repositoty.OrganizationSecurityPolicyRepository;
import com.ums.org.service.OrganizationAccessService;

@ExtendWith(MockitoExtension.class)
class OrganizationSecurityPolicyServiceImplTests {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private OrganizationSecurityPolicyRepository policyRepository;

	@Mock
	private OrganizationAccessService accessService;

	@Mock
	private AuditPublisher auditPublisher;

	@InjectMocks
	private OrganizationSecurityPolicyServiceImpl service;

	@Test
	void returnsDisabledDefaultForLegacyOrganizationWithoutPolicyRow() {
		UUID organizationId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, actorId);
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
		when(policyRepository.findById(organizationId)).thenReturn(Optional.empty());

		var response = service.getPolicy(organizationId, actorId, false);

		assertThat(response.organizationId()).isEqualTo(organizationId);
		assertThat(response.requireMfa()).isFalse();
		assertThat(response.updatedAt()).isNull();
		verify(accessService).assertCanViewOrganization(actorId, organization, false);
		verify(policyRepository, never()).save(any());
	}

	@Test
	void materializesPolicyOnFirstAuthorizedUpdate() {
		UUID organizationId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, actorId);
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
		when(policyRepository.findById(organizationId)).thenReturn(Optional.empty());
		when(policyRepository.save(any(OrganizationSecurityPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.updatePolicy(
				organizationId,
				new UpdateOrganizationSecurityPolicyRequest(true),
				actorId,
				false);

		ArgumentCaptor<OrganizationSecurityPolicy> captor = ArgumentCaptor.forClass(OrganizationSecurityPolicy.class);
		verify(policyRepository).save(captor.capture());
		assertThat(captor.getValue().getOrganizationId()).isEqualTo(organizationId);
		assertThat(captor.getValue().isRequireMfa()).isTrue();
		assertThat(captor.getValue().getUpdatedBy()).isEqualTo(actorId);
		assertThat(response.requireMfa()).isTrue();
		verify(accessService).assertCanManageMembers(actorId, organization, false);
		verify(auditPublisher).publish(any());
	}

	@Test
	void deniedActorCannotMutatePolicy() {
		UUID organizationId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, UUID.randomUUID());
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
		doThrow(new AccessDeniedException("denied"))
				.when(accessService).assertCanManageMembers(actorId, organization, false);

		assertThatThrownBy(() -> service.updatePolicy(
				organizationId,
				new UpdateOrganizationSecurityPolicyRequest(true),
				actorId,
				false))
				.isInstanceOf(AccessDeniedException.class);

		verify(policyRepository, never()).findById(organizationId);
		verify(policyRepository, never()).save(any());
	}

	@Test
	void internalReadReturnsPersistedPolicyAndActiveState() {
		UUID organizationId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, UUID.randomUUID());
		OrganizationSecurityPolicy policy = OrganizationSecurityPolicy.builder()
				.organizationId(organizationId)
				.requireMfa(true)
				.build();
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
		when(policyRepository.findById(organizationId)).thenReturn(Optional.of(policy));

		var response = service.getInternalPolicy(organizationId);

		assertThat(response.organizationId()).isEqualTo(organizationId);
		assertThat(response.requireMfa()).isTrue();
		assertThat(response.active()).isTrue();
	}

	@Test
	void internalReadUsesDisabledDefaultWithoutMaterializingLegacyRow() {
		UUID organizationId = UUID.randomUUID();
		Organization organization = activeOrganization(organizationId, UUID.randomUUID());
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
		when(policyRepository.findById(organizationId)).thenReturn(Optional.empty());

		var response = service.getInternalPolicy(organizationId);

		assertThat(response.requireMfa()).isFalse();
		assertThat(response.active()).isTrue();
		verify(policyRepository, never()).save(any());
	}

	private Organization activeOrganization(UUID organizationId, UUID ownerId) {
		return Organization.builder()
				.id(organizationId)
				.name("Security Policy Test")
				.slug("security-policy-test")
				.ownerId(ownerId)
				.status(OrganizationStatus.ACTIVE)
				.build();
	}
}
