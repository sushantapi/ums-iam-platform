package com.ums.notification.constant;

public final class QueueConstants {

	private QueueConstants() {
	}

	public static final String EXCHANGE = "ums.exchange";

	public static final String USER_QUEUE = "user.queue";
	public static final String AUTH_QUEUE = "auth.queue";
	public static final String ORG_QUEUE = "org.queue";

	public static final String USER_REGISTERED_KEY = "user.registered";
	public static final String EMAIL_VERIFIED_KEY = "user.email.verified";

	public static final String PASSWORD_RESET_KEY = "auth.password.reset";
	public static final String MFA_OTP_KEY = "auth.mfa.otp";

	public static final String ORG_INVITE_KEY = "organization.invited";
	public static final String ROLE_ASSIGNED_KEY = "role.assigned";

	// Dedicated queues
	/*
	 * public static final String EMAIL_VERIFICATION_QUEUE =
	 * "email.verification.queue";
	 * 
	 * public static final String PASSWORD_RESET_QUEUE = "password.reset.queue";
	 * 
	 * public static final String MFA_OTP_QUEUE = "mfa.otp.queue";
	 */

	public static final String USER_EVENTS_DLQ = "user.events.dlq";

	public static final String USER_DLQ = "user.dlq";

	public static final String USER_DLQ_KEY = "user.dlq";
	
	
	
}