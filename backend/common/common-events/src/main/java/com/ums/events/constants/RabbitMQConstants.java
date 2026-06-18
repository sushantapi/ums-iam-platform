package com.ums.events.constants;

public final class RabbitMQConstants {

	private RabbitMQConstants() {
	}

	/*
	 * =========================== Exchanges ===========================
	 */

	public static final String USER_EXCHANGE = "user.exchange";

	public static final String ORGANIZATION_EXCHANGE = "organization.exchange";

	public static final String ROLE_ASSIGNED = "role.assigned";

	public static final String ROLE_ASSIGNED_QUEUE = "role.assigned.queue";

	/*
	 * =========================== Routing Keys ===========================
	 */

	public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

	public static final String ORGANIZATION_CREATED_ROUTING_KEY = "organization.created";

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