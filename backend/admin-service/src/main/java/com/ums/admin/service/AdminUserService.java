package com.ums.admin.service;

import java.util.UUID;

import com.ums.admin.dto.request.AdminCreateUserRequest;
import com.ums.admin.dto.response.UserAccountResponse;
import com.ums.admin.dto.response.UserDetailResponse;
import com.ums.admin.dto.response.UserSummaryPageResponse;

public interface AdminUserService {

    UserAccountResponse createUser(AdminCreateUserRequest request, UUID actorUserId);

    UserSummaryPageResponse getUsers(int page, int size, String search);

    UserDetailResponse getUserById(UUID userId);

    void activateUser(UUID userId, UUID actorUserId);

    void suspendUser(UUID userId, UUID actorUserId);

    void unlockUser(UUID userId, UUID actorUserId);
}
