package com.ums.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.entity.AuditLog;
import com.ums.service.AuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	@GetMapping
	public Page<AuditLog> getAll(Pageable pageable) {

		return auditService.getAll(pageable);
	}
}