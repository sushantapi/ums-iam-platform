package com.ums.notification.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        if (event == null || event.getUserId() == null || event.getEmail() == null || event.getEmail().isBlank()) {
            throw new IllegalArgumentException("User registration event requires userId and email");
        }

        log.info("Processing user registration notification userId={}", event.getUserId());
        notificationService.processUserRegistered(event);
    }
}
