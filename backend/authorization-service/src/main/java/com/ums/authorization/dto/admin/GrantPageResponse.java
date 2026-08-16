package com.ums.authorization.dto.admin;

import java.util.List;

public record GrantPageResponse(
		List<UserRoleAssignmentResponse> content, int page, int size, long totalElements, int totalPages) {
}
