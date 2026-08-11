package com.ums.admin.dto.response;

import java.util.List;

public record UserSummaryPageResponse(
		List<UserSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
