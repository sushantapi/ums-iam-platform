package com.ums.org.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.events.event.AuditEvent;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.client.UserClient;
import com.ums.org.dto.AddMemberRequest;
import com.ums.org.dto.CreateOrganizationRequest;
import com.ums.org.dto.OrganizationMemberResponse;
import com.ums.org.dto.OrganizationResponse;
import com.ums.org.dto.UserResponse;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationMember;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.publisher.OrganizationEventPublisher;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.service.OrganizationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional

@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMemberRepository memberRepository;
	private final UserClient userClient;
	private final OrganizationEventPublisher eventPublisher;

	private final AuditPublisher auditPublisher;

	@Override
	public OrganizationResponse createOrganization(CreateOrganizationRequest request, UUID ownerId) {

		UserResponse user = userClient.getUser(ownerId);

		if (user == null) {
			throw new ResourceNotFoundException("Owner user not found");
		}

		String slug = generateUniqueSlug(request.name());

		Organization organization = Organization.builder().name(request.name().trim()).slug(slug)
				.description(request.description()).ownerId(ownerId).status(OrganizationStatus.ACTIVE).build();

		organization = organizationRepository.save(organization);

		OrganizationMember ownerMembership = OrganizationMember.builder().organizationId(organization.getId())
				.userId(ownerId).role(OrganizationRole.OWNER).joinedAt(LocalDateTime.now()).build();

		memberRepository.save(ownerMembership);

		// Business Event
		publishOrganizationCreatedEvent(organization, user.email());

		// Audit Event
		publishAuditEvent(AuditEvent.builder().eventType("organization.created").serviceName("organization-service")
				.userId(ownerId.toString()).userEmail(user.email()).action("ORGANIZATION_CREATE")
				.entityType("ORGANIZATION").entityId(organization.getId().toString())
				.details("Organization created successfully").timestamp(LocalDateTime.now()).build());

		return new OrganizationResponse(organization.getId(), organization.getName(), organization.getSlug(),
				organization.getDescription());

	}

	private void publishOrganizationCreatedEvent(Organization organization, String ownerEmail) {

		OrganizationCreatedEvent event = OrganizationCreatedEvent.builder().organizationId(organization.getId())
				.organizationName(organization.getName()).ownerId(organization.getOwnerId()).ownerEmail(ownerEmail)
				.createdAt(LocalDateTime.now()).build();

		eventPublisher.publishOrganizationCreated(event);

	}

	private void publishAuditEvent(AuditEvent event) {

		try {

			auditPublisher.publish(event);

		} catch (Exception ex) {

			log.error("Failed to publish audit event", ex);
		}

	}

	private String generateUniqueSlug(String name) {

		String baseSlug = name.trim().toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");

		String slug = baseSlug;
		int counter = 1;

		while (organizationRepository.findBySlug(slug).isPresent()) {
			slug = baseSlug + "-" + counter++;
		}

		return slug;
	}

	/*
	 * private void publishOrganizationCreatedEvent(Organization organization,
	 * String ownerEmail) {
	 * 
	 * OrganizationCreatedEvent event =
	 * OrganizationCreatedEvent.builder().organizationId(organization.getId())
	 * .organizationName(organization.getName()).ownerId(organization.getOwnerId()).
	 * ownerEmail(ownerEmail) .createdAt(LocalDateTime.now()).build();
	 * 
	 * eventPublisher.publishOrganizationCreated(event); }
	 */

	@Override
	public void addMember(UUID organizationId, AddMemberRequest request) {

		organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		UserResponse user = userClient.getUser(request.userId());

		if (user == null) {
			throw new ResourceNotFoundException("User not found");
		}

		boolean exists = memberRepository.existsByOrganizationIdAndUserId(organizationId, request.userId());

		if (exists) {
			throw new BadRequestException("User already belongs to organization");
		}

		OrganizationMember member = OrganizationMember.builder().organizationId(organizationId).userId(request.userId())
				.role(request.role()).joinedAt(LocalDateTime.now()).build();

		memberRepository.save(member);
	}

	@Override
	public OrganizationResponse getOrganization(UUID organizationId) {

		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		return new OrganizationResponse(organization.getId(), organization.getName(), organization.getSlug(),
				organization.getDescription());
	}

	@Override
	public List<OrganizationMemberResponse> getMembers(UUID organizationId) {

		organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		return memberRepository.findByOrganizationId(organizationId).stream()
				.map(member -> new OrganizationMemberResponse(member.getId(), member.getUserId(), member.getRole(),
						member.getJoinedAt()))
				.toList();
	}
}