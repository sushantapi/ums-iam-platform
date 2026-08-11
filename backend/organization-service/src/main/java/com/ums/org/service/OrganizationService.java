package com.ums.org.service;

import java.util.List;
import java.util.UUID;

import com.ums.org.dto.AddMemberRequest;
import com.ums.org.dto.CreateOrganizationRequest;
import com.ums.org.dto.OrganizationMemberResponse;
import com.ums.org.dto.OrganizationResponse;

public interface OrganizationService {

	OrganizationResponse createOrganization(CreateOrganizationRequest request, UUID ownerId);

	OrganizationResponse getOrganization(UUID organizationId, UUID actorUserId, boolean superAdmin);

	void addMember(UUID organizationId, AddMemberRequest request, UUID actorUserId);

	List<OrganizationMemberResponse> getMembers(UUID organizationId, UUID actorUserId);
	
	
	
}
