package com.scms.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditHistoryIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired AuditEventRepository audits;
	@Autowired AccountRepository accounts;
	@Autowired AuditHistoryService service;
	@Autowired JdbcTemplate jdbc;

	@Test
	void appliesCaseInsensitiveFiltersAndHalfOpenTimeWindow() {
		UUID managerId = createAccount(AccountRole.MANAGER);
		UUID actorId = createAccount(AccountRole.RECEPTIONIST);
		UUID targetId = UUID.randomUUID();
		Instant from = Instant.parse("2026-10-01T00:00:00Z");
		Instant to = Instant.parse("2026-10-02T00:00:00Z");

		UUID matchingId = UUID.randomUUID();
		UUID boundaryId = UUID.randomUUID();
		insertAuditEvent(matchingId, actorId, targetId, "confirmed", from.plusSeconds(1));
		insertAuditEvent(boundaryId, actorId, targetId, "boundary", to);

		AuditHistoryPageResponse result = service.list(managerId, actorId, "payment_paid", "attendance", targetId,
			from, to, PageRequest.of(0, 20));

		assertThat(result.content()).singleElement().satisfies(event -> {
			assertThat(event.id()).isEqualTo(matchingId);
			assertThat(event.actorAccountId()).isEqualTo(actorId);
			assertThat(event.actorName()).isEqualTo("RECEPTIONIST Audit User");
			assertThat(event.actorRole()).isEqualTo(AccountRole.RECEPTIONIST);
		});
	}

	@Test
	void auditHistoryIsAppendOnlyAtDatabaseBoundary() {
		UUID managerId = createAccount(AccountRole.MANAGER);
		UUID eventId = UUID.randomUUID();
		audits.saveAndFlush(new AuditEvent(eventId, managerId, "MANAGER_ACTION", "ACCOUNT", UUID.randomUUID(),
			Map.of("status", "ACTIVE")));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("delete from audit_events where id = ?", eventId))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("audit_events is append-only");
	}

	private void insertAuditEvent(UUID id, UUID actorId, UUID targetId, String reason, Instant createdAt) {
		jdbc.update("""
				insert into audit_events
				(id, actor_account_id, action, target_type, target_id, reason, before_data, after_data, created_at)
				values (?, ?, 'PAYMENT_PAID', 'Attendance', ?, ?, '{"status":"PENDING"}'::jsonb,
				'{"status":"PAID"}'::jsonb, ?)
				""", id, actorId, targetId, reason, Timestamp.from(createdAt));
	}

	private UUID createAccount(AccountRole role) {
		UUID id = UUID.randomUUID();
		Account account = new Account(id, role, AccountStatus.ACTIVE, role.name() + " Audit User",
			"090" + id.toString().replace("-", "").substring(0, 9), id + "@audit.example.test",
			LocalDate.of(1990, 1, 1), "{noop}audit-password");
		accounts.saveAndFlush(account);
		return id;
	}
}
