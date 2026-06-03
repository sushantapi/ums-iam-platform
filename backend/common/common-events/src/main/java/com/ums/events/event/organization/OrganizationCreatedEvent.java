package com.ums.events.event.organization;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationCreatedEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID organizationId;
	private String organizationName;
	private UUID ownerId;
	private String ownerEmail;
}