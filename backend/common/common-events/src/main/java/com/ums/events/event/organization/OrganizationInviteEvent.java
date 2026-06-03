package com.ums.events.event.organization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteEvent {

	private String email;
	private String organizationName;
	private String inviteLink;
}