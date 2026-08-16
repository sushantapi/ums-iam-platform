package com.ums.user.messaging;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.user.entity.UserProfile;
import com.ums.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventConsumer {

    private final UserProfileRepository userProfileRepository;

    @RabbitListener(queues = RabbitMQConstants.PROFILE_USER_REGISTERED_QUEUE)
    @Transactional
    public void handle(UserRegisteredEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Ignoring invalid user registration event without userId");
            return;
        }

        if (userProfileRepository.existsById(event.getUserId())) {
            log.debug("User profile already exists for {}, skipping duplicate event", event.getUserId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        UserProfile profile = UserProfile.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .createdAt(now)
                .updatedAt(now)
                .build();

        userProfileRepository.save(profile);
        log.info("Created user profile projection for {}", event.getUserId());
    }
}
