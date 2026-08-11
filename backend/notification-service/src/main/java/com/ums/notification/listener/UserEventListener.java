/*
 * package com.ums.notification.listener;
 * 
 * import org.springframework.amqp.rabbit.annotation.RabbitListener; import
 * org.springframework.stereotype.Service;
 * 
 * import com.ums.events.constants.RabbitMQConstants; import
 * com.ums.events.event.user.UserRegisteredEvent; import
 * com.ums.notification.service.EmailService;
 * 
 * import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
 * 
 * @Service
 * 
 * @RequiredArgsConstructor
 * 
 * @Slf4j public class UserEventListener {
 * 
 * private final EmailService emailService;
 * 
 * @RabbitListener(queues =
 * RabbitMQConstants.NOTIFICATION_USER_REGISTERED_QUEUE) public void
 * handleUserRegistered(UserRegisteredEvent event) {
 * 
 * log.info("User registration received for {}", event.getEmail());
 * 
 * emailService.sendWelcomeEmail(event.getEmail(), event.getFirstName()); } }
 */

package com.ums.notification.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

	private final EmailService emailService;

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_USER_REGISTERED_QUEUE)
	public void handle(UserRegisteredEvent event) {

		log.info("Received UserRegisteredEvent for userId={}", event.getUserId());

		emailService.sendWelcomeEmail(event.getEmail(), event.getFirstName());
	}
}
