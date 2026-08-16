package com.ums.auth.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ums.auth.dto.admin.AdminSessionFilter;
import com.ums.auth.dto.admin.AdminSessionResponse;

public interface AdminSessionService {

	Page<AdminSessionResponse> listSessions(AdminSessionFilter filter, Pageable pageable);

	List<AdminSessionResponse> listUserSessions(UUID userId);

	void revokeSession(UUID sessionId, UUID adminUserId);

	void revokeAllUserSessions(UUID userId, UUID adminUserId);
}
