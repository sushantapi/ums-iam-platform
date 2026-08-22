package com.ums.org.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.AcceptOrganizationInvitationRequest;
import com.ums.org.dto.OrganizationInvitationAcceptanceResponse;
import com.ums.org.service.OrganizationInvitationAcceptanceService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/invitations")
@RequiredArgsConstructor
@Tag(name = "Organization Invitation Acceptance")
public class OrganizationInvitationAcceptanceController {

	private final OrganizationInvitationAcceptanceService invitationAcceptanceService;

	@PostMapping("/accept")
	public ResponseEntity<OrganizationInvitationAcceptanceResponse> acceptInvitation(
			Authentication authentication,
			@Valid @RequestBody AcceptOrganizationInvitationRequest request) {
		return ResponseEntity.ok(invitationAcceptanceService.acceptInvitation(
				request.token(), authenticatedUserId(authentication)));
	}

	private UUID authenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}
}
