package com.ums.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteEvent {

	private Long organizationId;

	private String organizationName;

	private String invitedEmail;

	private String invitedBy;

	private String invitationLink;
}