package com.ums.events.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private String eventType;

	private String serviceName;

	private String userId;

	private String userEmail;

	private String action;

	private String entityType;

	private String entityId;

	private String details;

	private String ipAddress;

	private LocalDateTime timestamp;
}