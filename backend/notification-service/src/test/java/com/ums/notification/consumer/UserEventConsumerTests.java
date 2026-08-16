package com.ums.notification.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTests {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserEventConsumer consumer;

    @Test
    void forwardsValidUserRegisteredEvent() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(UUID.randomUUID())
                .email("new.user@ums.local")
                .firstName("New")
                .lastName("User")
                .build();

        consumer.handleUserRegistered(event);

        verify(notificationService).processUserRegistered(event);
    }

    @Test
    void rejectsMalformedEventBeforeNotificationProcessing() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(UUID.randomUUID())
                .email(" ")
                .firstName("Broken")
                .build();

        assertThatThrownBy(() -> consumer.handleUserRegistered(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId and email");

        verifyNoInteractions(notificationService);
    }
}
