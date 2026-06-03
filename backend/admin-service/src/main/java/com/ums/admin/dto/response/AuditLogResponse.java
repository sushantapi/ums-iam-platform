package com.ums.admin.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

	private String auditId;

	private String action;

	private String username;

	private String userEmail;

	private String serviceName;

	private String module;

	private String endpoint;

	private String method;

	private String ipAddress;

	private String status;

	private String details;

	private LocalDateTime createdAt;
}