package com.ums.org.service;

import java.util.UUID;

import com.ums.org.dto.OrganizationSecurityPolicyInternalResponse;
import com.ums.org.dto.OrganizationSecurityPolicyResponse;
import com.ums.org.dto.UpdateOrganizationSecurityPolicyRequest;

public interface OrganizationSecurityPolicyService {

	OrganizationSecurityPolicyResponse getPolicy(UUID organizationId, UUID actorUserId, boolean superAdmin);

	OrganizationSecurityPolicyResponse updatePolicy(UUID organizationId, UpdateOrganizationSecurityPolicyRequest request,
			UUID actorUserId, boolean superAdmin);

	OrganizationSecurityPolicyInternalResponse getInternalPolicy(UUID organizationId);
}
