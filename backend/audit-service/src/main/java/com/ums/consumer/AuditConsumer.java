package com.ums.consumer;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.entity.AuditLog;
import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.AuditEvent;
import com.ums.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditConsumer {

	private final AuditLogRepository repository;

	@RabbitListener(queues = RabbitMQConstants.AUDIT_QUEUE)
	public void consume(AuditEvent event) {

		AuditLog auditLog = AuditLog.builder().eventType(event.getEventType()).serviceName(event.getServiceName())
				.userId(event.getUserId()).userEmail(event.getUserEmail()).action(event.getAction())
				.entityType(event.getEntityType()).entityId(event.getEntityId()).details(event.getDetails())
				.ipAddress(event.getIpAddress()).createdAt(event.getTimestamp()).build();

		repository.save(auditLog);
	}
}