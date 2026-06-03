package com.ums.org.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.org.dto.AddMemberRequest;
import com.ums.org.dto.CreateOrganizationRequest;
import com.ums.org.dto.OrganizationMemberResponse;
import com.ums.org.dto.OrganizationResponse;
import com.ums.org.service.OrganizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization Management")
public class OrganizationController {

	private final OrganizationService organizationService;

	@PostMapping
	@Operation(summary = "Create organization")
	public ResponseEntity<OrganizationResponse> createOrganization(@RequestHeader("X-User-Id") UUID ownerId,
			@Valid @RequestBody CreateOrganizationRequest request) {

		return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(request, ownerId));
	}

	@GetMapping("/{organizationId}")
	public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID organizationId) {

		return ResponseEntity.ok(organizationService.getOrganization(organizationId));
	}

	@PostMapping("/{organizationId}/members")
	public ResponseEntity<Void> addMember(@PathVariable UUID organizationId,
			@Valid @RequestBody AddMemberRequest request) {

		organizationService.addMember(organizationId, request);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/{organizationId}/members")
	public ResponseEntity<List<OrganizationMemberResponse>> getMembers(@PathVariable UUID organizationId) {

		return ResponseEntity.ok(organizationService.getMembers(organizationId));
	}
}