package com.scms.backend.notification;

@FunctionalInterface
public interface NotificationProvider {

	void deliver(NotificationOutboxEvent event);
}
