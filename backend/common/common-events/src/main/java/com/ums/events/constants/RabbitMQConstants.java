package com.ums.events.constants;

public final class RabbitMQConstants {

	private RabbitMQConstants() {
	}

	/*
	 * =========================== Exchanges ===========================
	 */

	public static final String USER_EXCHANGE = "user.exchange";

	public static final String ORGANIZATION_EXCHANGE = "organization.exchange";

	public static final String AUTH_EXCHANGE = "auth.exchange";

	public static final String NOTIFICATION_FAILURE_EXCHANGE = "notification.failure.exchange";

	public static final String ROLE_ASSIGNED = "role.assigned";

	public static final String ROLE_ASSIGNED_QUEUE = "role.assigned.queue";

	public static final String AUTH_ROLE_REVOKED_QUEUE = "authentication.role.revoked.queue";

	/*
	 * =========================== Routing Keys ===========================
	 */

	public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

	public static final String ORGANIZATION_CREATED_ROUTING_KEY = "organization.created";

	public static final String ORGANIZATION_INVITATION_ROUTING_KEY = "organization.invitation.created";

	public static final String EMAIL_VERIFICATION_ROUTING_KEY = "auth.email.verification.requested";

	public static final String PASSWORD_RESET_ROUTING_KEY = "auth.password.reset.requested";

	public static final String NOTIFICATION_PASSWORD_RESET_FAILURE_ROUTING_KEY = "notification.password.reset.failed";

	public static final String MFA_OTP_ROUTING_KEY = "auth.mfa.otp.requested";

	public static final String ROLE_REVOKED_ROUTING_KEY = "role.revoked";

	/*
	 * =========================== Profile Service Queues
	 * ===========================
	 */

	public static final String PROFILE_USER_REGISTERED_QUEUE = "profile.user.registered.queue";

	/*
	 * =========================== Notification Service Queues
	 * ===========================
	 */

	public static final String NOTIFICATION_USER_REGISTERED_QUEUE = "notification.user.registered.queue";

	public static final String NOTIFICATION_ORGANIZATION_CREATED_QUEUE = "notification.organization.created.queue";

	public static final String NOTIFICATION_ORGANIZATION_INVITATION_QUEUE = "notification.organization.invitation.queue";

	public static final String NOTIFICATION_EMAIL_VERIFICATION_QUEUE = "notification.email.verification.queue";

	public static final String NOTIFICATION_PASSWORD_RESET_QUEUE = "notification.password.reset.queue";

	public static final String NOTIFICATION_PASSWORD_RESET_DLQ = "notification.password.reset.dlq";

	public static final String NOTIFICATION_MFA_OTP_QUEUE = "notification.mfa.otp.queue";

	/*
	 * =========================== Organization Service Queues
	 * ===========================
	 */

	public static final String ORGANIZATION_CREATED_QUEUE = "organization.created.queue";

	/*
	 * Audit Constants
	 */

	public static final String AUDIT_EXCHANGE = "audit.exchange";

	public static final String AUDIT_QUEUE = "audit.queue";

	public static final String AUDIT_ROUTING_KEY = "audit.#";

}
