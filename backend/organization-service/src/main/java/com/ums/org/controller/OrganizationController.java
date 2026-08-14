package com.ums.org.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.AddMemberRequest;
import com.ums.org.dto.CreateOrganizationRequest;
import com.ums.org.dto.OrganizationMemberResponse;
import com.ums.org.dto.OrganizationResponse;
import com.ums.org.dto.UpdateOrganizationRequest;
import com.ums.org.service.OrganizationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization Management")
public class OrganizationController {

	private final OrganizationService organizationService;

	/*
	 * @PostMapping
	 * 
	 * @Operation(summary = "Create organization") public
	 * ResponseEntity<OrganizationResponse>
	 * createOrganization(@RequestHeader("X-User-Id") UUID ownerId,
	 * 
	 * @Valid @RequestBody CreateOrganizationRequest request) {
	 * 
	 * return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.
	 * createOrganization(request, ownerId)); }
	 */
	@PostMapping
	public ResponseEntity<OrganizationResponse> createOrganization(
			Authentication authentication,
			@Valid @RequestBody CreateOrganizationRequest request) {

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(organizationService.createOrganization(request, authenticatedUserId(authentication)));
	}

	@GetMapping("/{organizationId}")
	public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID organizationId,
			Authentication authentication) {

		return ResponseEntity.ok(organizationService.getOrganization(organizationId, authenticatedUserId(authentication),
				isSuperAdmin(authentication)));
	}

	@PutMapping("/{organizationId}")
	public ResponseEntity<OrganizationResponse> updateOrganization(@PathVariable UUID organizationId,
			Authentication authentication,
			@Valid @RequestBody UpdateOrganizationRequest request) {

		return ResponseEntity.ok(organizationService.updateOrganization(organizationId, request,
				authenticatedUserId(authentication), isSuperAdmin(authentication)));
	}

	@PostMapping("/{organizationId}/members")
	public ResponseEntity<Void> addMember(@PathVariable UUID organizationId,
			Authentication authentication,
			@Valid @RequestBody AddMemberRequest request) {

		organizationService.addMember(organizationId, request, authenticatedUserId(authentication), isSuperAdmin(authentication));

		return ResponseEntity.ok().build();
	}

	@GetMapping("/{organizationId}/members")
	public ResponseEntity<List<OrganizationMemberResponse>> getMembers(@PathVariable UUID organizationId,
			Authentication authentication) {

		return ResponseEntity.ok(organizationService.getMembers(organizationId, authenticatedUserId(authentication), isSuperAdmin(authentication)));
	}

	@DeleteMapping("/{organizationId}/members/{userId}")
	public ResponseEntity<Void> removeMember(@PathVariable UUID organizationId,
			@PathVariable UUID userId, Authentication authentication) {

		organizationService.removeMember(organizationId, userId, authenticatedUserId(authentication), isSuperAdmin(authentication));
		return ResponseEntity.noContent().build();
	}

	private UUID authenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}

	private boolean isSuperAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
	}
}
