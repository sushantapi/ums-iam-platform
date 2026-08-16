package com.ums.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AuditEventPageResponse(
		List<AuditEventResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static AuditEventPageResponse from(Page<AuditEventResponse> page) {
		return new AuditEventPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
