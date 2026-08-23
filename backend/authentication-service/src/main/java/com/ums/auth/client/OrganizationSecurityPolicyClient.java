package com.ums.auth.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ums.auth.dto.OrganizationSecurityPolicyResponse;

@FeignClient(name = "organization-service")
public interface OrganizationSecurityPolicyClient {

	@GetMapping("/api/v1/internal/organizations/{organizationId}/security-policy")
	OrganizationSecurityPolicyResponse getSecurityPolicy(@PathVariable UUID organizationId);
}
