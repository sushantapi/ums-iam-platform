package com.ums.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptOrganizationInvitationRequest(
		@NotBlank(message = "Invitation token is required")
		@Size(max = 512, message = "Invitation token is invalid")
		String token) {

	@Override
	public String toString() {
		return "AcceptOrganizationInvitationRequest[token=<redacted>]";
	}
}
