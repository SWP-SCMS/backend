package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResendEmailProviderTests {
	private HttpServer server;
	private final AtomicReference<Reply> reply = new AtomicReference<>(new Reply(200, "{\"id\":\"email-123\"}"));
	private final List<String> bodies = new ArrayList<>();
	private final List<String> keys = new ArrayList<>();
	private ResendEmailProvider provider;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/emails", this::handle);
		server.start();
		EmailDeliveryProperties properties = validProperties();
		provider = new ResendEmailProvider(properties, new EmailPayloadCodec());
	}

	@AfterEach
	void tearDown() { server.stop(0); }

	@Test
	void replaysExactlyTheSamePayloadAndIdempotencyKey() {
		UUID outboxId = UUID.randomUUID();
		String payload = "{\"from\":\"SCMS <no-reply@example.test>\",\"to\":[\"member@example.test\"],"
			+ "\"subject\":\"OTP\",\"html\":\"<b>123456</b>\",\"text\":\"123456\"}";
		EmailMessage message = new EmailMessage("SCMS <no-reply@example.test>", List.of("member@example.test"),
			"OTP", "<b>123456</b>", "123456");
		EmailDeliveryRequest request = new EmailDeliveryRequest(outboxId, payload, message);

		assertThat(provider.send(request).providerMessageId()).isEqualTo("email-123");
		assertThat(provider.send(request).providerMessageId()).isEqualTo("email-123");
		assertThat(bodies).containsExactly(payload, payload);
		assertThat(keys).containsExactly("scms-email-outbox/" + outboxId, "scms-email-outbox/" + outboxId);
	}

	@Test
	void requiresProviderMessageIdBeforeAccepting() {
		reply.set(new Reply(200, "{}"));
		assertThatThrownBy(() -> provider.send(request()))
			.isInstanceOfSatisfying(EmailDeliveryException.class, error -> {
				assertThat(error.retryable()).isTrue();
				assertThat(error.errorName()).isEqualTo("RESEND_MISSING_MESSAGE_ID");
			});
	}

	@Test
	void classifiesStructuredIdempotencyErrorsByName() {
		reply.set(new Reply(409, "{\"name\":\"concurrent_idempotent_requests\",\"message\":\"busy\"}"));
		assertThatThrownBy(() -> provider.send(request()))
			.isInstanceOfSatisfying(EmailDeliveryException.class, error -> {
				assertThat(error.retryable()).isTrue();
				assertThat(error.errorName()).isEqualTo("concurrent_idempotent_requests");
			});

		reply.set(new Reply(409, "{\"name\":\"invalid_idempotent_request\",\"message\":\"changed\"}"));
		assertThatThrownBy(() -> provider.send(request()))
			.isInstanceOfSatisfying(EmailDeliveryException.class, error -> {
				assertThat(error.retryable()).isFalse();
				assertThat(error.errorName()).isEqualTo("invalid_idempotent_request");
			});
	}

	@Test
	void unknownOrMalformedConflictIsTerminal() {
		reply.set(new Reply(409, "{\"code\":\"concurrent_idempotent_requests\"}"));
		assertThatThrownBy(() -> provider.send(request()))
			.isInstanceOfSatisfying(EmailDeliveryException.class, error -> {
				assertThat(error.retryable()).isFalse();
				assertThat(error.errorName()).isEqualTo("RESEND_UNKNOWN_409");
			});
	}

	@Test
	void resendConfigurationFailsFastWithoutApiKeyOrSender() {
		EmailDeliveryProperties missing = new EmailDeliveryProperties();
		missing.setProvider("resend");
		assertThatThrownBy(missing::validate).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("RESEND_API_KEY").hasMessageContaining("RESEND_FROM");
	}

	private EmailDeliveryRequest request() {
		return new EmailDeliveryRequest(UUID.randomUUID(), "{}",
			new EmailMessage("from@example.test", List.of("to@example.test"), "subject", "html", "text"));
	}

	private EmailDeliveryProperties validProperties() {
		EmailDeliveryProperties properties = new EmailDeliveryProperties();
		properties.setProvider("resend");
		properties.getResend().setApiKey("re_test");
		properties.getResend().setFrom("SCMS <no-reply@example.test>");
		properties.getResend().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
		properties.getResend().setConnectTimeout(Duration.ofSeconds(2));
		properties.getResend().setReadTimeout(Duration.ofSeconds(2));
		properties.validate();
		return properties;
	}

	private void handle(HttpExchange exchange) throws IOException {
		bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
		Reply current = reply.get();
		byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(current.status(), body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private record Reply(int status, String body) { }
}
