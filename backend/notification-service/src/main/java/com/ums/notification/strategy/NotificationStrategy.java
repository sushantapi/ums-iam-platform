package com.ums.notification.strategy;

import com.ums.notification.domain.NotificationContext;
import com.ums.notification.enums.NotificationChannel;

public interface NotificationStrategy {

	NotificationChannel getChannel();

	void send(NotificationContext context);

}