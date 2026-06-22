package com.ums.auth.dto.admin;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminSessionPageResponse(
		List<AdminSessionResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static AdminSessionPageResponse from(Page<AdminSessionResponse> page) {
		return new AdminSessionPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
