package com.ums.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.request.AdminAddOrganizationMemberRequest;
import com.ums.admin.dto.request.AdminCreateOrganizationRequest;
import com.ums.admin.dto.request.AdminUpdateOrganizationRequest;
import com.ums.admin.dto.response.OrganizationAdminPageResponse;
import com.ums.admin.dto.response.OrganizationAdminResponse;
import com.ums.admin.dto.response.OrganizationMemberResponse;
import com.ums.admin.service.AdminOrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','SUPPORT') or hasAuthority('ORGANIZATION_READ')")
public class AdminOrganizationController {

	private final AdminOrganizationService adminOrganizationService;

	@PostMapping
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ORGANIZATION_WRITE')")
	public ResponseEntity<OrganizationAdminResponse> create(
			@Valid @RequestBody AdminCreateOrganizationRequest request,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(adminOrganizationService.create(request, currentAdminId(authentication)));
	}

	@GetMapping
	public OrganizationAdminPageResponse list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String search) {
		return adminOrganizationService.list(page, size, search);
	}

	@GetMapping("/{organizationId}")
	public OrganizationAdminResponse get(@PathVariable UUID organizationId) {
		return adminOrganizationService.get(organizationId);
	}

	@GetMapping("/{organizationId}/members")
	public List<OrganizationMemberResponse> members(@PathVariable UUID organizationId, Authentication authentication) {
		return adminOrganizationService.getMembers(organizationId, currentAdminId(authentication));
	}

	@PutMapping("/{organizationId}")
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ORGANIZATION_WRITE')")
	public OrganizationAdminResponse update(@PathVariable UUID organizationId,
			@Valid @RequestBody AdminUpdateOrganizationRequest request, Authentication authentication) {
		return adminOrganizationService.update(organizationId, request, currentAdminId(authentication));
	}

	@PostMapping("/{organizationId}/members")
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ORGANIZATION_WRITE')")
	public ResponseEntity<Void> addMember(@PathVariable UUID organizationId,
			@Valid @RequestBody AdminAddOrganizationMemberRequest request, Authentication authentication) {
		adminOrganizationService.addMember(organizationId, request, currentAdminId(authentication));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{organizationId}/members/{userId}")
	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ORGANIZATION_WRITE')")
	public ResponseEntity<Void> removeMember(@PathVariable UUID organizationId, @PathVariable UUID userId,
			Authentication authentication) {
		adminOrganizationService.removeMember(organizationId, userId, currentAdminId(authentication));
		return ResponseEntity.noContent().build();
	}

	private UUID currentAdminId(Authentication authentication) {
		return (UUID) authentication.getPrincipal();
	}
}
