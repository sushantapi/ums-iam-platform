package com.ums.user.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record UserSummaryPageResponse(
		List<UserSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static UserSummaryPageResponse from(Page<UserSummaryResponse> page) {
		return new UserSummaryPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
