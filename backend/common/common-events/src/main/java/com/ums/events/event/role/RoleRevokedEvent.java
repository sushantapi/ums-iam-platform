package com.ums.events.event.role;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRevokedEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID eventId;

	private UUID assignmentId;

	private UUID userId;

	private UUID roleId;

	private String roleName;

	private UUID revokedBy;

	private LocalDateTime revokedAt;
}
