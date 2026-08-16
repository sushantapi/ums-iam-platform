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

	private Long id;

	private String auditId;

	private String eventId;

	private String eventType;

	private String action;

	private String actor;

	private String target;

	private String username;

	private String userEmail;

	private String serviceName;

	private String entityType;

	private String entityId;

	private String ipAddress;

	private String outcome;

	private String details;

	private LocalDateTime timestamp;

	private LocalDateTime createdAt;
}
