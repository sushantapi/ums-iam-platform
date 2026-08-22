package com.ums.org.service;

import java.util.List;
import java.util.UUID;

import com.ums.org.dto.AddMemberRequest;
import com.ums.org.dto.CreateOrganizationInvitationRequest;
import com.ums.org.dto.CreateOrganizationRequest;
import com.ums.org.dto.OrganizationInvitationResponse;
import com.ums.org.dto.OrganizationMemberResponse;
import com.ums.org.dto.OrganizationResponse;
import com.ums.org.dto.UpdateOrganizationRequest;
import com.ums.org.dto.admin.OrganizationAdminPageResponse;
import com.ums.org.dto.admin.OrganizationAdminResponse;

public interface OrganizationService {

	OrganizationResponse createOrganization(CreateOrganizationRequest request, UUID ownerId);

	OrganizationResponse getOrganization(UUID organizationId, UUID actorUserId, boolean superAdmin);

	OrganizationResponse updateOrganization(UUID organizationId, UpdateOrganizationRequest request, UUID actorUserId,
			boolean superAdmin);

	void addMember(UUID organizationId, AddMemberRequest request, UUID actorUserId, boolean superAdmin);

	List<OrganizationMemberResponse> getMembers(UUID organizationId, UUID actorUserId, boolean superAdmin);

	void removeMember(UUID organizationId, UUID userId, UUID actorUserId, boolean superAdmin);

	OrganizationInvitationResponse createInvitation(UUID organizationId, CreateOrganizationInvitationRequest request,
			UUID actorUserId, boolean superAdmin);

	List<OrganizationInvitationResponse> getInvitations(UUID organizationId, UUID actorUserId, boolean superAdmin);

	OrganizationAdminPageResponse listOrganizations(int page, int size, String search);

	OrganizationAdminPageResponse listOrganizationsForActor(int page, int size, String search, UUID actorUserId,
			boolean superAdmin);

	OrganizationAdminResponse getOrganizationForAdmin(UUID organizationId);

	OrganizationAdminResponse getOrganizationForAdmin(UUID organizationId, UUID actorUserId, boolean superAdmin);

	List<OrganizationAdminResponse> getOrganizationsForUser(UUID userId);
}
