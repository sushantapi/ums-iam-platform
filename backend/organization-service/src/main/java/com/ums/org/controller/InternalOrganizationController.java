package com.ums.org.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.AddMemberRequest;
import com.ums.org.dto.OrganizationMemberResponse;
import com.ums.org.dto.UpdateOrganizationRequest;
import com.ums.org.dto.admin.OrganizationAdminPageResponse;
import com.ums.org.dto.admin.OrganizationAdminResponse;
import com.ums.org.dto.admin.OrganizationMetricsResponse;
import com.ums.org.service.OrganizationService;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.enums.OrganizationStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalOrganizationController {

	private static final String ACTOR_HEADER = "X-Actor-User-Id";
	private final OrganizationService organizationService;
	private final OrganizationRepository organizationRepository;

	@GetMapping("/metrics")
	public OrganizationMetricsResponse getMetrics() {
		return new OrganizationMetricsResponse(
				organizationRepository.count(),
				organizationRepository.countByStatus(OrganizationStatus.ACTIVE),
				0L);
	}

	@GetMapping
	public OrganizationAdminPageResponse list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String search) {
		return organizationService.listOrganizations(page, size, search);
	}

	@GetMapping("/{organizationId}")
	public OrganizationAdminResponse get(@PathVariable UUID organizationId) {
		return organizationService.getOrganizationForAdmin(organizationId);
	}

	@GetMapping("/by-user/{userId}")
	public List<OrganizationAdminResponse> byUser(@PathVariable UUID userId) {
		return organizationService.getOrganizationsForUser(userId);
	}

	@GetMapping("/{organizationId}/members")
	public List<OrganizationMemberResponse> members(@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId) {
		return organizationService.getMembers(organizationId, actorUserId, true);
	}

	@PutMapping("/{organizationId}")
	public OrganizationAdminResponse update(@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@Valid @RequestBody UpdateOrganizationRequest request) {
		organizationService.updateOrganization(organizationId, request, actorUserId, true);
		return organizationService.getOrganizationForAdmin(organizationId);
	}

	@PostMapping("/{organizationId}/members")
	public ResponseEntity<Void> addMember(@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@Valid @RequestBody AddMemberRequest request) {
		organizationService.addMember(organizationId, request, actorUserId, true);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{organizationId}/members/{userId}")
	public ResponseEntity<Void> removeMember(@PathVariable UUID organizationId, @PathVariable UUID userId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId) {
		organizationService.removeMember(organizationId, userId, actorUserId, true);
		return ResponseEntity.noContent().build();
	}
}
