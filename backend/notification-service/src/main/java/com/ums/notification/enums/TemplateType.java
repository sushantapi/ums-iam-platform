package com.ums.notification.enums;

public enum TemplateType {

	WELCOME_EMAIL("welcome-email"),

	EMAIL_VERIFICATION("email-verification"),

	PASSWORD_RESET("password-reset"),

	MFA_OTP("mfa-otp"),

	ORGANIZATION_INVITATION("organization-invitation");

	private final String templateName;

	TemplateType(String templateName) {
		this.templateName = templateName;
	}

	public String getTemplateName() {
		return templateName;
	}
}