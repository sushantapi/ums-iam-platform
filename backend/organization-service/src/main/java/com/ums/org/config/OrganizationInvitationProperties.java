package com.ums.org.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "organization.invitation")
@Validated
@Getter
@Setter
public class OrganizationInvitationProperties {

	@Min(1)
	@Max(720)
	private long expiryHours = 72;
}
