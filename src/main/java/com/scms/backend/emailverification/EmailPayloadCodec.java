package com.scms.backend.emailverification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
class EmailPayloadCodec {
	private final ObjectMapper json = new ObjectMapper();

	String serialize(EmailMessage message) {
		try { return json.writeValueAsString(message); }
		catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to snapshot email payload", exception); }
	}

	EmailMessage deserialize(String payload) {
		try { return json.readValue(payload, EmailMessage.class); }
		catch (JsonProcessingException exception) { throw EmailDeliveryException.terminal("INVALID_EMAIL_SNAPSHOT"); }
	}

	String textField(String payload, String field) {
		try {
			if (payload == null || payload.isBlank()) return null;
			JsonNode body = json.readTree(payload);
			if (body == null || !body.hasNonNull(field) || !body.get(field).isTextual()) return null;
			return body.get(field).asText();
		} catch (JsonProcessingException exception) {
			return null;
		}
	}
}
