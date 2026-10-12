package com.scms.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;

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
		audits.saveAndFlush(new AuditEvent(matchingId, actorId, "PAYMENT_PAID", "Attendance", targetId,
			"confirmed", Map.of("status", "PENDING"), Map.of("status", "PAID")));
		jdbc.update("update audit_events set created_at = ? where id = ?", from.plusSeconds(1), matchingId);
		audits.saveAndFlush(new AuditEvent(boundaryId, actorId, "PAYMENT_PAID", "Attendance", targetId,
			"boundary", Map.of("status", "PENDING"), Map.of("status", "PAID")));
		jdbc.update("update audit_events set created_at = ? where id = ?", to, boundaryId);

		AuditHistoryPageResponse result = service.list(managerId, actorId, "payment_paid", "attendance", targetId,
			from, to, PageRequest.of(0, 20));

		assertThat(result.content()).extracting(AuditHistoryResponse::id).containsExactly(matchingId);
	}

	@Test
	void auditHistoryIsAppendOnlyAtDatabaseBoundary() {
		UUID managerId = createAccount(AccountRole.MANAGER);
		UUID eventId = UUID.randomUUID();
		audits.saveAndFlush(new AuditEvent(eventId, managerId, "MANAGER_ACTION", "ACCOUNT", UUID.randomUUID(),
			Map.of("status", "ACTIVE")));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("delete from audit_events where id = ?", eventId))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
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
