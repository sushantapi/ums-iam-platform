package com.ums.org.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.OrganizationSecurityPolicyResponse;
import com.ums.org.dto.UpdateOrganizationSecurityPolicyRequest;
import com.ums.org.service.OrganizationSecurityPolicyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/security-policy")
@RequiredArgsConstructor
public class OrganizationSecurityPolicyController {

	private final OrganizationSecurityPolicyService securityPolicyService;

	@GetMapping
	public ResponseEntity<OrganizationSecurityPolicyResponse> getPolicy(
			@PathVariable UUID organizationId,
			Authentication authentication) {
		return ResponseEntity.ok(securityPolicyService.getPolicy(
				organizationId,
				authenticatedUserId(authentication),
				isSuperAdmin(authentication)));
	}

	@PutMapping
	public ResponseEntity<OrganizationSecurityPolicyResponse> updatePolicy(
			@PathVariable UUID organizationId,
			Authentication authentication,
			@Valid @RequestBody UpdateOrganizationSecurityPolicyRequest request) {
		return ResponseEntity.ok(securityPolicyService.updatePolicy(
				organizationId,
				request,
				authenticatedUserId(authentication),
				isSuperAdmin(authentication)));
	}

	private UUID authenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}

	private boolean isSuperAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
	}
}
