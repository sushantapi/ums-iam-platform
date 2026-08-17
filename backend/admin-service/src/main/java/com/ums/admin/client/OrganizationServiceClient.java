package com.ums.admin.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.ums.admin.dto.request.AdminAddOrganizationMemberRequest;
import com.ums.admin.dto.request.AdminCreateOrganizationRequest;
import com.ums.admin.dto.request.AdminUpdateOrganizationRequest;
import com.ums.admin.dto.response.OrganizationAdminPageResponse;
import com.ums.admin.dto.response.OrganizationAdminResponse;
import com.ums.admin.dto.response.OrganizationMemberResponse;
import com.ums.admin.dto.response.OrganizationMetricsResponse;

@FeignClient(name = "organization-service", contextId = "organizationAdminClient")
public interface OrganizationServiceClient {

	String ACTOR_HEADER = "X-Actor-User-Id";
	String SUPER_ADMIN_HEADER = "X-Actor-Super-Admin";

	@GetMapping("/api/v1/internal/organizations/metrics")
	OrganizationMetricsResponse getMetrics();

	@PostMapping("/api/v1/internal/organizations")
	OrganizationAdminResponse create(
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestBody AdminCreateOrganizationRequest request);

	@GetMapping("/api/v1/internal/organizations")
	OrganizationAdminPageResponse list(
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestHeader(SUPER_ADMIN_HEADER) boolean superAdmin,
			@RequestParam int page,
			@RequestParam int size,
			@RequestParam(required = false) String search);

	@GetMapping("/api/v1/internal/organizations/{organizationId}")
	OrganizationAdminResponse get(
			@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestHeader(SUPER_ADMIN_HEADER) boolean superAdmin);

	@GetMapping("/api/v1/internal/organizations/by-user/{userId}")
	List<OrganizationAdminResponse> byUser(@PathVariable UUID userId);

	@GetMapping("/api/v1/internal/organizations/{organizationId}/members")
	List<OrganizationMemberResponse> members(
			@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestHeader(SUPER_ADMIN_HEADER) boolean superAdmin);

	@PutMapping("/api/v1/internal/organizations/{organizationId}")
	OrganizationAdminResponse update(
			@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestHeader(SUPER_ADMIN_HEADER) boolean superAdmin,
			@RequestBody AdminUpdateOrganizationRequest request);

	@PostMapping("/api/v1/internal/organizations/{organizationId}/members")
	void addMember(
			@PathVariable UUID organizationId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestHeader(SUPER_ADMIN_HEADER) boolean superAdmin,
			@RequestBody AdminAddOrganizationMemberRequest request);

	@DeleteMapping("/api/v1/internal/organizations/{organizationId}/members/{userId}")
	void removeMember(
			@PathVariable UUID organizationId,
			@PathVariable UUID userId,
			@RequestHeader(ACTOR_HEADER) UUID actorUserId,
			@RequestHeader(SUPER_ADMIN_HEADER) boolean superAdmin);
}
