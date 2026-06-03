/*
 * package com.ums.notification.consumer;
 * 
 * import org.springframework.amqp.core.Message; import
 * org.springframework.amqp.rabbit.annotation.RabbitListener; import
 * org.springframework.stereotype.Component;
 * 
 * import com.ums.events.event.UserRegisteredEvent; import
 * com.ums.notification.constant.QueueConstants; import
 * com.ums.notification.service.NotificationService;
 * 
 * import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
 * 
 * @Slf4j
 * 
 * @Component
 * 
 * @RequiredArgsConstructor public class UserEventConsumer {
 * 
 * private final NotificationService notificationService;
 * 
 * @RabbitListener(queues = QueueConstants.USER_QUEUE) public void
 * handleUserRegistered(UserRegisteredEvent event) {
 * 
 * log.info("Received UserRegisteredEvent: {}", event);
 * 
 * notificationService.processUserRegistered(event); }
 * 
 * @RabbitListener(queues = QueueConstants.USER_EVENTS_DLQ) public void
 * processFailedMessages(Message message) {
 * 
 * log.error("DLQ Message Received: {}", new String(message.getBody())); } }
 * 
 * package com.ums.notification.consumer;
 * 
 * import org.springframework.amqp.rabbit.annotation.RabbitListener; import
 * org.springframework.stereotype.Component;
 * 
 * import com.ums.events.event.user.UserRegisteredEvent; import
 * com.ums.notification.constant.QueueConstants; import
 * com.ums.notification.service.NotificationService;
 * 
 * import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
 * 
 * @Slf4j
 * 
 * @Component
 * 
 * @RequiredArgsConstructor public class UserEventConsumer {
 * 
 * private final NotificationService notificationService;
 * 
 * @RabbitListener(queues = QueueConstants.USER_QUEUE) public void
 * handleUserRegistered(UserRegisteredEvent event) {
 * 
 * log.info("========================================");
 * log.info("USER REGISTERED EVENT RECEIVED"); log.info("User Id    : {}",
 * event.getUserId()); log.info("Email      : {}", event.getEmail());
 * log.info("First Name : {}", event.getFirstName());
 * log.info("========================================");
 * 
 * notificationService.processUserRegistered(event); } }
 */