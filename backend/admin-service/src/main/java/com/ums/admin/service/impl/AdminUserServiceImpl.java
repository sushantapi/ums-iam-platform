package com.ums.admin.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.admin.client.AuthenticationServiceClient;
import com.ums.admin.client.UserServiceClient;
import com.ums.admin.dto.request.AdminCreateUserRequest;
import com.ums.admin.dto.response.UserAccountResponse;
import com.ums.admin.dto.response.UserDetailResponse;
import com.ums.admin.dto.response.UserSummaryPageResponse;
import com.ums.admin.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserServiceClient userServiceClient;
    private final AuthenticationServiceClient authenticationServiceClient;

    @Override
    public UserAccountResponse createUser(AdminCreateUserRequest request, UUID actorUserId) {
        return authenticationServiceClient.create(actorUserId, request);
    }

    @Override
    public UserSummaryPageResponse getUsers(int page, int size, String search) {
        return userServiceClient.getUsers(page, size, search);
    }

    @Override
    public UserDetailResponse getUserById(UUID userId) {
        var profile = userServiceClient.getUser(userId);
        var account = authenticationServiceClient.getUser(userId);

        return UserDetailResponse.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .active("ACTIVE".equals(account.status()))
                .status(account.status())
                .locked(account.locked())
                .lockedUntil(account.lockedUntil())
                .lastLoginAt(account.lastLoginAt())
                .build();
    }

    @Override
    public void activateUser(UUID userId, UUID actorUserId) {
        authenticationServiceClient.activate(userId, actorUserId);
    }

    @Override
    public void suspendUser(UUID userId, UUID actorUserId) {
        authenticationServiceClient.suspend(userId, actorUserId);
    }

    @Override
    public void unlockUser(UUID userId, UUID actorUserId) {
        authenticationServiceClient.unlock(userId, actorUserId);
    }
}
