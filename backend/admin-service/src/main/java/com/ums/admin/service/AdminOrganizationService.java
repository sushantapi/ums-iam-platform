package com.ums.admin.service;

import java.util.List;
import java.util.UUID;

import com.ums.admin.dto.request.AdminAddOrganizationMemberRequest;
import com.ums.admin.dto.request.AdminCreateOrganizationRequest;
import com.ums.admin.dto.request.AdminUpdateOrganizationRequest;
import com.ums.admin.dto.response.OrganizationAdminPageResponse;
import com.ums.admin.dto.response.OrganizationAdminResponse;
import com.ums.admin.dto.response.OrganizationMemberResponse;

public interface AdminOrganizationService {

	OrganizationAdminResponse create(AdminCreateOrganizationRequest request, UUID actorUserId);

	OrganizationAdminPageResponse list(int page, int size, String search, UUID actorUserId, boolean superAdmin);

	OrganizationAdminResponse get(UUID organizationId, UUID actorUserId, boolean superAdmin);

	List<OrganizationAdminResponse> getForUser(UUID userId);

	List<OrganizationMemberResponse> getMembers(UUID organizationId, UUID actorUserId, boolean superAdmin);

	OrganizationAdminResponse update(UUID organizationId, AdminUpdateOrganizationRequest request, UUID actorUserId,
			boolean superAdmin);

	void addMember(UUID organizationId, AdminAddOrganizationMemberRequest request, UUID actorUserId,
			boolean superAdmin);

	void removeMember(UUID organizationId, UUID userId, UUID actorUserId, boolean superAdmin);
}
