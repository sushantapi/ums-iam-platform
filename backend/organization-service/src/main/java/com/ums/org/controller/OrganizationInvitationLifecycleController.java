package com.ums.org.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.OrganizationInvitationResponse;
import com.ums.org.service.OrganizationInvitationLifecycleService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invitations")
@RequiredArgsConstructor
@Tag(name = "Organization Invitation Lifecycle")
public class OrganizationInvitationLifecycleController {

	private final OrganizationInvitationLifecycleService invitationLifecycleService;

	@PostMapping("/{invitationId}/resend")
	public ResponseEntity<OrganizationInvitationResponse> resendInvitation(
			@PathVariable UUID organizationId,
			@PathVariable UUID invitationId,
			Authentication authentication) {
		return ResponseEntity.ok(invitationLifecycleService.resendInvitation(
				organizationId, invitationId, authenticatedUserId(authentication), isSuperAdmin(authentication)));
	}

	@PostMapping("/{invitationId}/revoke")
	public ResponseEntity<OrganizationInvitationResponse> revokeInvitation(
			@PathVariable UUID organizationId,
			@PathVariable UUID invitationId,
			Authentication authentication) {
		return ResponseEntity.ok(invitationLifecycleService.revokeInvitation(
				organizationId, invitationId, authenticatedUserId(authentication), isSuperAdmin(authentication)));
	}

	private UUID authenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}

	private boolean isSuperAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
	}
}
