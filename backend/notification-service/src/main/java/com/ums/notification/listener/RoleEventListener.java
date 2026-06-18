package com.ums.notification.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.role.RoleAssignedEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RoleEventListener {

	@RabbitListener(queues = RabbitMQConstants.ROLE_ASSIGNED_QUEUE)
	public void consume(RoleAssignedEvent event) {

		log.info("====================================");
		log.info("ROLE ASSIGNED EVENT RECEIVED");
		log.info("User Id   : {}", event.getUserId());
		log.info("Role Name : {}", event.getRoleName());
		log.info("Assigned  : {}", event.getAssignedAt());
		log.info("====================================");
	}
}