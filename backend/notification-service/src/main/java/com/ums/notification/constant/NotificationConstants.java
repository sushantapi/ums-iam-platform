package com.ums.notification.constant;

public final class NotificationConstants {

	private NotificationConstants() {
	}

	// Notification Status
	public static final String SENT = "SENT";
	public static final String FAILED = "FAILED";
	public static final String PENDING = "PENDING";

	// Template Names
	public static final String WELCOME_EMAIL = "WELCOME_EMAIL";
	public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
	public static final String PASSWORD_RESET = "PASSWORD_RESET";
	public static final String MFA_OTP = "MFA_OTP";
	public static final String ORGANIZATION_INVITATION = "ORGANIZATION_INVITATION";

	// Subjects
	public static final String WELCOME_SUBJECT = "Welcome to UMS IAM Platform";

	public static final String EMAIL_VERIFICATION_SUBJECT = "Verify Your Email Address";

	public static final String PASSWORD_RESET_SUBJECT = "Reset Your Password";

	public static final String MFA_OTP_SUBJECT = "Your Verification Code";

	public static final String ORG_INVITATION_SUBJECT = "Organization Invitation";
	
	
	
	

}