package com.ums.admin.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

	private long totalUsers;

	private long activeUsers;

	private long blockedUsers;

	private long activeSessions;

	private long todayLogins;
}