package com.ums.events.event.organization;

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
public class OrganizationMfaRequiredEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID eventId;

	private UUID organizationId;

	private UUID updatedBy;

	private LocalDateTime occurredAt;
}
