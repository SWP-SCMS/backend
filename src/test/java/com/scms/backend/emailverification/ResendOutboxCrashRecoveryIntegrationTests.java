package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"scms.email-verification.enabled=true",
	"scms.email-verification.hmac-active-version=v1",
	"scms.email-verification.hmac-keys=v1:MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
	"scms.email-verification.encryption-active-version=v1",
	"scms.email-verification.encryption-keys=v1:YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=",
	"scms.email-verification.rate-limit-key=cmF0ZS1saW1pdC1rZXktMzItYnl0ZXMtbG9uZyEhISE=",
	"scms.email.provider=resend",
	"scms.email.resend.api-key=re_test",
	"scms.email.resend.from=SCMS <no-reply@example.test>",
	"scms.email-verification.dispatch-delay=3600000",
	"scms.email-verification.dispatch-initial-delay=3600000"
})
class ResendOutboxCrashRecoveryIntegrationTests {
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
	private static final HttpServer resend = server();
	private static final List<String> bodies = new CopyOnWriteArrayList<>();
	private static final List<String> keys = new CopyOnWriteArrayList<>();

	@Autowired EmailOutboxEnqueuer outbox;
	@Autowired EmailOutboxDeliveryService delivery;
	@Autowired AccountRepository accounts;
	@Autowired PasswordEncoder passwords;
	@Autowired JdbcTemplate db;

	@DynamicPropertySource
	static void resendProperties(DynamicPropertyRegistry registry) {
		registry.add("scms.email.resend.base-url", () -> "http://127.0.0.1:" + resend.getAddress().getPort());
	}

	@AfterAll
	static void stopServer() { resend.stop(0); }

	@Test
	void crashAfterProviderAcceptReclaimsJobWithoutSendingADifferentRequest() {
		UUID accountId = UUID.randomUUID();
		accounts.saveAndFlush(new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Crash Recovery",
			"09" + accountId.toString().replace("-", "").substring(0, 8), "crash@example.test",
			LocalDate.of(2000, 1, 1), passwords.encode("Secret123!")));
		Instant now = Instant.now();
		UUID outboxId = outbox.enqueue("TEST_RESEND_CRASH:" + accountId, accountId, 1, "crash@example.test",
			EmailMessageRenderer.VERIFICATION, "123456", now.plusSeconds(600), now);

		var first = delivery.claim(now, "worker-before-crash").orElseThrow();
		assertThat(first.id()).isEqualTo(outboxId);
		assertThat(delivery.deliver(first, now).providerMessageId()).isEqualTo("resend-email-1");
		// Simulated crash: intentionally do not call delivery.sent(...).

		Instant afterLockTimeout = now.plusSeconds(360);
		var reclaimed = delivery.claim(afterLockTimeout, "worker-after-crash").orElseThrow();
		assertThat(reclaimed.id()).isEqualTo(outboxId);
		var replayed = delivery.deliver(reclaimed, afterLockTimeout);
		delivery.sent(outboxId, replayed.providerMessageId(), afterLockTimeout);

		assertThat(bodies).hasSize(2).allMatch(bodies.getFirst()::equals);
		assertThat(keys).containsExactly("scms-email-outbox/" + outboxId, "scms-email-outbox/" + outboxId);
		assertThat(db.queryForObject("select status from email_outbox where id=?", String.class, outboxId))
			.isEqualTo("SENT");
		assertThat(db.queryForObject("select provider_message_id from email_outbox where id=?", String.class, outboxId))
			.isEqualTo("resend-email-1");
		assertThat(db.queryForObject("select retry_count from email_outbox where id=?", Integer.class, outboxId))
			.isEqualTo(2);
	}

	private static HttpServer server() {
		try {
			HttpServer value = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			value.createContext("/emails", ResendOutboxCrashRecoveryIntegrationTests::accept);
			value.start();
			return value;
		} catch (IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static void accept(HttpExchange exchange) throws IOException {
		bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
		byte[] response = "{\"id\":\"resend-email-1\"}".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
