package com.ums.auth.service;

import java.util.UUID;

import com.ums.auth.dto.admin.AdminUserAccountResponse;
import com.ums.auth.dto.admin.AdminUserMetricsResponse;

public interface AdminUserAccountService {

	AdminUserAccountResponse getUser(UUID userId);

	AdminUserMetricsResponse getMetrics();

	void activateUser(UUID userId, UUID actorUserId);

	void suspendUser(UUID userId, UUID actorUserId);

	void unlockUser(UUID userId, UUID actorUserId);
}
