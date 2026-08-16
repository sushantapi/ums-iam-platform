package com.ums.admin.dto.response;

import java.util.List;

public record OrganizationAdminPageResponse(
		List<OrganizationAdminResponse> content, int page, int size, long totalElements, int totalPages) {
}
