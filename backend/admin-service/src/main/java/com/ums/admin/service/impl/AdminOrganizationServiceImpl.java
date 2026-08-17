package com.ums.admin.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.admin.client.OrganizationServiceClient;
import com.ums.admin.dto.request.AdminAddOrganizationMemberRequest;
import com.ums.admin.dto.request.AdminCreateOrganizationRequest;
import com.ums.admin.dto.request.AdminUpdateOrganizationRequest;
import com.ums.admin.dto.response.OrganizationAdminPageResponse;
import com.ums.admin.dto.response.OrganizationAdminResponse;
import com.ums.admin.dto.response.OrganizationMemberResponse;
import com.ums.admin.service.AdminOrganizationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

	private final OrganizationServiceClient organizationServiceClient;

	@Override
	public OrganizationAdminResponse create(AdminCreateOrganizationRequest request, UUID actorUserId) {
		return organizationServiceClient.create(actorUserId, request);
	}

	@Override
	public OrganizationAdminPageResponse list(int page, int size, String search, UUID actorUserId, boolean superAdmin) {
		return organizationServiceClient.list(actorUserId, superAdmin, page, size, search);
	}

	@Override
	public OrganizationAdminResponse get(UUID organizationId, UUID actorUserId, boolean superAdmin) {
		return organizationServiceClient.get(organizationId, actorUserId, superAdmin);
	}

	@Override
	public List<OrganizationAdminResponse> getForUser(UUID userId) {
		return organizationServiceClient.byUser(userId);
	}

	@Override
	public List<OrganizationMemberResponse> getMembers(UUID organizationId, UUID actorUserId, boolean superAdmin) {
		return organizationServiceClient.members(organizationId, actorUserId, superAdmin);
	}

	@Override
	public OrganizationAdminResponse update(UUID organizationId, AdminUpdateOrganizationRequest request, UUID actorUserId,
			boolean superAdmin) {
		return organizationServiceClient.update(organizationId, actorUserId, superAdmin, request);
	}

	@Override
	public void addMember(UUID organizationId, AdminAddOrganizationMemberRequest request, UUID actorUserId,
			boolean superAdmin) {
		organizationServiceClient.addMember(organizationId, actorUserId, superAdmin, request);
	}

	@Override
	public void removeMember(UUID organizationId, UUID userId, UUID actorUserId, boolean superAdmin) {
		organizationServiceClient.removeMember(organizationId, userId, actorUserId, superAdmin);
	}
}
