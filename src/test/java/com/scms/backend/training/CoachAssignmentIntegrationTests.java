package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.notification.NotificationOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CoachAssignmentIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	private CoachAssignmentService service;

	@Autowired
	private AccountRepository accounts;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private NotificationOutboxDispatcher notificationOutboxDispatcher;

	private Account createAccount(AccountRole role, AccountStatus status, String name) {
		UUID id = UUID.randomUUID();
		String unique = id.toString().substring(0, 8);
		Account account = new Account(id, role, status, name, "09" + unique,
				name.toLowerCase().replace(" ", "") + unique + "@example.test",
				LocalDate.of(1990, 1, 1), "{noop}secret");
		return accounts.saveAndFlush(account);
	}

	private void createMembership(UUID memberId, UUID actorId, String planCode, Instant startsAt, Instant endsAt) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();

		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days, status, created_by_account_id)
			values (?, ?, ?, 'Test offer', 1000, 30, 'ACTIVE', ?)
			""", offerId, planCode, "Offer " + offerId, actorId);

		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				payment_method, status, paid_at)
			values (?, ?, ?, ?, ?, 'Offer', ?, 1000, 'VND', 30, 'CASH', 'PAID', ?)
			""", orderId, "ORD-" + orderId, memberId, actorId, offerId, planCode, Timestamp.from(startsAt));

		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code, processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 1000, 'VND', ?, ?)
			""", UUID.randomUUID(), orderId, actorId, Timestamp.from(startsAt));

		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot, offer_name_snapshot,
				price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, status, starts_at, ends_at)
			values (?, ?, ?, ?, ?, 'Offer', 1000, 'VND', 30, 'ACTIVE', ?, ?)
			""", membershipId, memberId, orderId, offerId, planCode, Timestamp.from(startsAt), Timestamp.from(endsAt));
	}

	@Test
	void assignAndReassignSuccess_persistsToDatabase_auditsAndNotifies() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Alpha");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member Bravo");
		Account coachA = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Charlie");
		Account coachB = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Delta");

		Instant now = Instant.now();
		createMembership(member.getId(), manager.getId(), "PLUS", now.minusSeconds(86400), now.plusSeconds(86400 * 30));

		// 1. Assign coachA
		CoachAssignmentResponse responseA = service.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coachA.getId(), "Initial coach assignment"));

		assertThat(responseA).isNotNull();
		assertThat(responseA.coachAccountId()).isEqualTo(coachA.getId());
		assertThat(responseA.status()).isEqualTo(CoachAssignmentStatus.ACTIVE);

		// Verify database row
		Map<String, Object> assignmentRowA = jdbc.queryForMap(
				"select * from coach_assignments where id = ?", responseA.id());
		assertThat(assignmentRowA.get("member_account_id")).isEqualTo(member.getId());
		assertThat(assignmentRowA.get("coach_account_id")).isEqualTo(coachA.getId());
		assertThat(assignmentRowA.get("status")).isEqualTo("ACTIVE");
		assertThat(assignmentRowA.get("ends_at")).isNull();

		// Verify audit event
		List<Map<String, Object>> auditsA = jdbc.queryForList(
				"select * from audit_events where target_id = ? and action = 'COACH_ASSIGNED'", responseA.id());
		assertThat(auditsA).hasSize(1);
		assertThat(auditsA.get(0).get("actor_account_id")).isEqualTo(manager.getId());

		// Verify notifications (Member & Coach A)
		notificationOutboxDispatcher.dispatchAvailable();
		Integer notifCountA = jdbc.queryForObject(
				"select count(*) from notifications where target_id = ? and notification_type = 'TRAINING_COACH_ASSIGNED'",
				Integer.class, responseA.id());
		assertThat(notifCountA).isEqualTo(2);

		// 2. Reassign to coachB
		CoachAssignmentResponse responseB = service.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coachB.getId(), "Reassigning to coach B"));

		assertThat(responseB).isNotNull();
		assertThat(responseB.coachAccountId()).isEqualTo(coachB.getId());
		assertThat(responseB.status()).isEqualTo(CoachAssignmentStatus.ACTIVE);

		// Verify old assignment row is now ENDED
		Map<String, Object> updatedRowA = jdbc.queryForMap(
				"select * from coach_assignments where id = ?", responseA.id());
		assertThat(updatedRowA.get("status")).isEqualTo("ENDED");
		assertThat(updatedRowA.get("ends_at")).isNotNull();
		assertThat(updatedRowA.get("end_reason")).isEqualTo("Reassigning to coach B");

		// Verify new assignment row is ACTIVE
		Map<String, Object> rowB = jdbc.queryForMap(
				"select * from coach_assignments where id = ?", responseB.id());
		assertThat(rowB.get("status")).isEqualTo("ACTIVE");
		assertThat(rowB.get("ends_at")).isNull();

		// Verify audit event for reassignment
		List<Map<String, Object>> auditsB = jdbc.queryForList(
				"select * from audit_events where target_id = ? and action = 'COACH_REASSIGNED'", responseB.id());
		assertThat(auditsB).hasSize(1);
	}

	@Test
	void assignFails_whenMemberHasOnlyBasicMembership() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Beta");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member Gamma");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Echo");

		Instant now = Instant.now();
		createMembership(member.getId(), manager.getId(), "BASIC", now.minusSeconds(86400), now.plusSeconds(86400 * 30));

		assertThatThrownBy(() -> service.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "Attempt assignment on Basic")))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "PLUS_MEMBERSHIP_REQUIRED");
	}

	@Test
	void assignFails_whenCoachInactive() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Gamma");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member Delta");
		Account inactiveCoach = createAccount(AccountRole.COACH, AccountStatus.INACTIVE, "Coach Foxtrot");

		Instant now = Instant.now();
		createMembership(member.getId(), manager.getId(), "PLUS", now.minusSeconds(86400), now.plusSeconds(86400 * 30));

		assertThatThrownBy(() -> service.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(inactiveCoach.getId(), "Attempt assignment with inactive coach")))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "COACH_NOT_ACTIVE");
	}

	@Test
	void assignFails_whenMemberSuspended() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Delta");
		Account suspendedMember = createAccount(AccountRole.MEMBER, AccountStatus.SUSPENDED, "Member Epsilon");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Golf");

		Instant now = Instant.now();
		createMembership(suspendedMember.getId(), manager.getId(), "PLUS", now.minusSeconds(86400), now.plusSeconds(86400 * 30));

		assertThatThrownBy(() -> service.assign(manager.getId(), suspendedMember.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "Attempt assignment with suspended member")))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "MEMBER_NOT_ACTIVE");
	}

	@Test
	void endAssignment_updatesDatabaseRow_auditsAndNotifies() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Epsilon");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member Zeta");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Hotel");

		Instant now = Instant.now();
		createMembership(member.getId(), manager.getId(), "PLUS", now.minusSeconds(86400), now.plusSeconds(86400 * 30));

		CoachAssignmentResponse assigned = service.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "Start program"));

		CoachAssignmentResponse ended = service.endAssignment(manager.getId(), member.getId(),
				new CoachAssignmentEndRequest("Program completed by mutual agreement"));

		assertThat(ended.status()).isEqualTo(CoachAssignmentStatus.ENDED);
		assertThat(ended.endReason()).isEqualTo("Program completed by mutual agreement");

		Map<String, Object> row = jdbc.queryForMap(
				"select status, ends_at, end_reason from coach_assignments where id = ?", assigned.id());
		assertThat(row.get("status")).isEqualTo("ENDED");
		assertThat(row.get("ends_at")).isNotNull();
		assertThat(row.get("end_reason")).isEqualTo("Program completed by mutual agreement");

		List<Map<String, Object>> audits = jdbc.queryForList(
				"select * from audit_events where target_id = ? and action = 'COACH_ASSIGNMENT_ENDED'", assigned.id());
		assertThat(audits).hasSize(1);
	}

	@Test
	void databaseUniqueConstraint_preventsTwoActiveAssignmentsConcurrently() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Zeta");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member Eta");
		Account coach1 = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach India");
		Account coach2 = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Juliet");

		Instant now = Instant.now();
		createMembership(member.getId(), manager.getId(), "PLUS", now.minusSeconds(86400), now.plusSeconds(86400 * 30));

		service.assign(manager.getId(), member.getId(), new CoachAssignmentAssignRequest(coach1.getId(), "First"));

		// Direct SQL insert attempting to create another ACTIVE assignment for same member
		assertThatThrownBy(() -> jdbc.update("""
			insert into coach_assignments (id, member_account_id, coach_account_id, assigned_by_account_id, status, starts_at)
			values (?, ?, ?, ?, 'ACTIVE', ?)
			""", UUID.randomUUID(), member.getId(), coach2.getId(), manager.getId(), Timestamp.from(now)))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uq_coach_assignments_active_member");
	}

	@Test
	void expiredPlusMembership_transitionsActiveCoachAssignmentToEnded_andRetainsHistory() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Expiry");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member Expiry");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Expiry");

		Instant now = Instant.now();
		Instant membershipStartsAt = now.minusSeconds(86400 * 30);
		Instant membershipEndsAt = now.minusSeconds(3600); // Expired 1 hour ago
		createMembership(member.getId(), manager.getId(), "PLUS", membershipStartsAt, membershipEndsAt);

		UUID assignmentId = UUID.randomUUID();
		Instant assignmentStartsAt = now.minusSeconds(86400 * 20); // Assigned while PLUS was active
		jdbc.update("""
			insert into coach_assignments (id, member_account_id, coach_account_id, assigned_by_account_id,
				status, starts_at, ends_at, end_reason, created_at, updated_at, version)
			values (?, ?, ?, ?, 'ACTIVE', ?, null, null, ?, ?, 0)
			""", assignmentId, member.getId(), coach.getId(), manager.getId(),
			Timestamp.from(assignmentStartsAt), Timestamp.from(assignmentStartsAt), Timestamp.from(assignmentStartsAt));

		// 1. Querying active coach assignment triggers lifecycle transition
		java.util.Optional<CoachAssignmentResponse> activeResult = service.getActive(manager.getId(), member.getId());
		assertThat(activeResult).isEmpty();

		// 2. Verify database record is actually ENDED in PostgreSQL, not just hidden from query
		Map<String, Object> assignmentRow = jdbc.queryForMap(
				"select status, ends_at, end_reason from coach_assignments where id = ?", assignmentId);
		assertThat(assignmentRow.get("status")).isEqualTo("ENDED");
		assertThat(assignmentRow.get("ends_at")).isNotNull();
		assertThat(assignmentRow.get("end_reason")).isEqualTo("PLUS membership expired");

		// 3. Verify history retains the ended assignment
		CoachAssignmentPageResponse history = service.listHistory(manager.getId(), member.getId(),
				org.springframework.data.domain.PageRequest.of(0, 10));
		assertThat(history.totalElements()).isEqualTo(1);
		assertThat(history.content().get(0).id()).isEqualTo(assignmentId);
		assertThat(history.content().get(0).status()).isEqualTo(CoachAssignmentStatus.ENDED);
		assertThat(history.content().get(0).endReason()).isEqualTo("PLUS membership expired");
	}
}
