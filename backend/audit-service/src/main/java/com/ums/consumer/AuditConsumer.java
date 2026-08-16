package com.ums.consumer;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.AuditEvent;
import com.ums.repository.AuditLogRepository;
import com.ums.service.AuditEventSanitizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditConsumer {

	private final AuditLogRepository repository;
	private final AuditEventSanitizer sanitizer;

	@RabbitListener(queues = RabbitMQConstants.AUDIT_QUEUE)
	public void consume(AuditEvent event) {
		repository.save(sanitizer.sanitize(event));
	}
}
