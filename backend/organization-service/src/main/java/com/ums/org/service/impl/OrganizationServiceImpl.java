package com.ums.org.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import com.ums.org.dto.UpdateOrganizationRequest;
import com.ums.org.dto.admin.OrganizationAdminPageResponse;
import com.ums.org.dto.admin.OrganizationAdminResponse;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationMember;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.publisher.OrganizationEventPublisher;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.service.OrganizationAccessService;
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
	private final OrganizationAccessService accessService;
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
	public OrganizationResponse updateOrganization(UUID organizationId, UpdateOrganizationRequest request, UUID actorUserId, boolean superAdmin) {

		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

		if (request.name() != null) {
			String name = request.name().trim();
			if (name.isEmpty()) {
				throw new BadRequestException("Organization name must not be blank");
			}
			organization.setName(name);
		}

		if (request.description() != null) {
			organization.setDescription(request.description().trim());
		}

		Organization updated = organizationRepository.save(organization);

		publishAuditEvent(AuditEvent.builder().eventType("organization.updated").serviceName("organization-service")
				.userId(actorUserId.toString()).action("ORGANIZATION_UPDATE").entityType("ORGANIZATION")
				.entityId(updated.getId().toString()).details("Organization updated successfully")
				.timestamp(LocalDateTime.now()).build());

		return new OrganizationResponse(updated.getId(), updated.getName(), updated.getSlug(), updated.getDescription());
	}

	@Override
	public void addMember(UUID organizationId, AddMemberRequest request, UUID actorUserId, boolean superAdmin) {

		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

		if (request.role() == OrganizationRole.OWNER) {
			throw new BadRequestException("Owner assignment requires an ownership transfer flow");
		}

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

		member = memberRepository.save(member);

		publishAuditEvent(AuditEvent.builder().eventType("organization.member.added")
				.serviceName("organization-service").userId(actorUserId.toString()).action("ORGANIZATION_MEMBER_ADD")
				.entityType("ORGANIZATION_MEMBER").entityId(member.getId().toString())
				.details("Added user " + request.userId() + " to organization " + organizationId
						+ " as " + request.role().name())
				.timestamp(LocalDateTime.now()).build());
	}

	@Override
	public OrganizationResponse getOrganization(UUID organizationId, UUID actorUserId, boolean superAdmin) {

		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		accessService.assertCanViewOrganization(actorUserId, organization, superAdmin);

		return new OrganizationResponse(organization.getId(), organization.getName(), organization.getSlug(),
				organization.getDescription());
	}

	@Override
	public List<OrganizationMemberResponse> getMembers(UUID organizationId, UUID actorUserId, boolean superAdmin) {

		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

		return memberRepository.findByOrganizationId(organizationId).stream()
				.map(member -> new OrganizationMemberResponse(member.getId(), member.getUserId(), member.getRole(),
						member.getJoinedAt()))
				.toList();
	}

	@Override
	public void removeMember(UUID organizationId, UUID userId, UUID actorUserId, boolean superAdmin) {

		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

		if (organization.getOwnerId().equals(userId)) {
			throw new BadRequestException("Organization owner cannot be removed from membership");
		}

		OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization member not found"));

		memberRepository.delete(member);

		publishAuditEvent(AuditEvent.builder().eventType("organization.member.removed")
				.serviceName("organization-service").userId(actorUserId.toString()).action("ORGANIZATION_MEMBER_REMOVE")
				.entityType("ORGANIZATION_MEMBER").entityId(member.getId().toString())
				.details("Removed user " + userId + " from organization " + organizationId)
				.timestamp(LocalDateTime.now()).build());
	}

	@Override
	public OrganizationAdminPageResponse listOrganizations(int page, int size, String search) {
		if (page < 0 || page > 100_000 || size < 1 || size > 200) {
			throw new BadRequestException("Invalid page or size");
		}
		if (search != null && search.length() > 255) {
			throw new BadRequestException("search must not exceed 255 characters");
		}

		String query = search == null ? "" : escapeSearch(search.trim().toLowerCase(java.util.Locale.ROOT));
		var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
		var organizations = query.isBlank()
				? organizationRepository.findAll(pageable)
				: organizationRepository.search(query, pageable);

		return new OrganizationAdminPageResponse(
				organizations.getContent().stream().map(this::toAdminResponse).toList(),
				organizations.getNumber(), organizations.getSize(), organizations.getTotalElements(),
				organizations.getTotalPages());
	}

	@Override
	public OrganizationAdminResponse getOrganizationForAdmin(UUID organizationId) {
		return toAdminResponse(organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found")));
	}

	@Override
	public List<OrganizationAdminResponse> getOrganizationsForUser(UUID userId) {
		List<UUID> organizationIds = memberRepository.findByUserId(userId).stream()
				.map(OrganizationMember::getOrganizationId).distinct().toList();
		return organizationRepository.findAllById(organizationIds).stream().map(this::toAdminResponse).toList();
	}

	private OrganizationAdminResponse toAdminResponse(Organization organization) {
		return new OrganizationAdminResponse(organization.getId(), organization.getName(), organization.getSlug(),
				organization.getDescription(), organization.getOwnerId(), organization.getStatus().name());
	}

	private String escapeSearch(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
