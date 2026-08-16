package com.ums.notification.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.constant.QueueConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Profile("dev")
public class RabbitTestController {

	private final RabbitTemplate rabbitTemplate;

	@PostMapping("/publish")
	public String publish() {

		UserRegisteredEvent event = UserRegisteredEvent.builder().email("sushant843120@gmail.com").firstName("Sushant")
				.lastName("Kumar").build();

		log.info("Publishing dev UserRegisteredEvent");

		rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.USER_REGISTERED_KEY, event);

		return "Event Published Successfully";
	}
}
