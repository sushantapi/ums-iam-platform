package com.ums.org.dto;

import java.util.List;

public record UserSummaryPageResponse(
		List<UserSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
