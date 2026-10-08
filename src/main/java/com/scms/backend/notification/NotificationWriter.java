package com.scms.backend.notification;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationWriter {

	private final NotificationRepository repository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	NotificationWriter(NotificationRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void write(String eventKey, UUID recipientId, String notificationType, String targetType, UUID targetId,
			Map<String, Object> payload) {
		try {
			repository.insertPending(UUID.randomUUID(), eventKey, recipientId, notificationType, targetType, targetId,
				objectMapper.writeValueAsString(payload));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Notification payload cannot be serialized", exception);
		}
	}
}
