package com.scms.backend.emailverification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ResendEmailProvider implements EmailProvider {
	private static final String CONCURRENT = "concurrent_idempotent_requests";
	private static final String INVALID = "invalid_idempotent_request";

	private final EmailDeliveryProperties properties;
	private final EmailPayloadCodec codec;
	private final HttpClient client;

	@Autowired
	ResendEmailProvider(EmailDeliveryProperties properties, EmailPayloadCodec codec) {
		this(properties, codec, HttpClient.newBuilder()
			.connectTimeout(properties.getResend().getConnectTimeout()).build());
	}

	ResendEmailProvider(EmailDeliveryProperties properties, EmailPayloadCodec codec, HttpClient client) {
		this.properties = properties;
		this.codec = codec;
		this.client = client;
	}

	@Override
	public String providerName() { return "resend"; }

	@Override
	public EmailDeliveryResult send(EmailDeliveryRequest delivery) {
		HttpRequest request = HttpRequest.newBuilder(endpoint())
			.timeout(properties.getResend().getReadTimeout())
			.header("Authorization", "Bearer " + properties.getResend().getApiKey())
			.header("Content-Type", "application/json")
			.header("Idempotency-Key", "scms-email-outbox/" + delivery.outboxId())
			.POST(HttpRequest.BodyPublishers.ofString(delivery.payloadJson()))
			.build();
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return classify(response);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw EmailDeliveryException.retryable("RESEND_INTERRUPTED", null, exception);
		} catch (IOException | RuntimeException exception) {
			if (exception instanceof EmailDeliveryException deliveryException) throw deliveryException;
			throw EmailDeliveryException.retryable("RESEND_CONNECTION_FAILED", null, exception);
		}
	}

	private EmailDeliveryResult classify(HttpResponse<String> response) {
		int status = response.statusCode();
		if (status >= 200 && status < 300) {
			String id = codec.textField(response.body(), "id");
			if (id == null || id.isBlank()) throw EmailDeliveryException.retryable("RESEND_MISSING_MESSAGE_ID");
			return EmailDeliveryResult.accepted(id);
		}
		String name = codec.textField(response.body(), "name");
		if (status == 409 && CONCURRENT.equals(name))
			throw EmailDeliveryException.retryable(name, retryAfter(response), null);
		if (status == 409 && INVALID.equals(name)) throw EmailDeliveryException.terminal(name);
		if (status == 409) throw EmailDeliveryException.terminal(name == null ? "RESEND_UNKNOWN_409" : name);
		if (status == 429 || status >= 500)
			throw EmailDeliveryException.retryable(name == null ? "RESEND_HTTP_" + status : name,
				retryAfter(response), null);
		throw EmailDeliveryException.terminal(name == null ? "RESEND_HTTP_" + status : name);
	}

	private Duration retryAfter(HttpResponse<?> response) {
		return response.headers().firstValue("Retry-After").flatMap(value -> {
			try { return java.util.Optional.of(Duration.ofSeconds(Long.parseLong(value.trim()))); }
			catch (NumberFormatException exception) { return java.util.Optional.empty(); }
		}).orElse(null);
	}

	private URI endpoint() {
		String base = properties.getResend().getBaseUrl();
		return URI.create((base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/emails");
	}
}
