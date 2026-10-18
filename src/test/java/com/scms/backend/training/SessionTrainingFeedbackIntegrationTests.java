package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class SessionTrainingFeedbackIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired SessionTrainingFeedbackService feedback;
	@Autowired SessionTrainingResultService results;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void teachingCoachCreatesAndUpdatesSessionFeedbackWithAuditHistory() {
		Account manager = createAccount(AccountRole.MANAGER, "Manager US49");
		Account coach = createAccount(AccountRole.COACH, "Teaching Coach US49");
		Account otherCoach = createAccount(AccountRole.COACH, "Other Coach US49");
		Account member = createAccount(AccountRole.MEMBER, "Member US49");
		UUID sessionId = createBookedYogaSession(manager.getId(), coach.getId(), member.getId());
		Instant activityAt = Instant.parse("2030-01-01T03:00:00Z");
		SessionTrainingResultResponse result = results.create(coach.getId(), sessionId, member.getId(),
			new SessionTrainingResultRequest(activityAt, Map.of("posesCompleted", 8)));

		SessionTrainingFeedbackResponse created = feedback.create(coach.getId(), sessionId, member.getId(),
			new SessionTrainingFeedbackRequest(result.id(), "Good alignment"));
		SessionTrainingFeedbackResponse updated = feedback.update(coach.getId(), sessionId, member.getId(),
			created.id(), new SessionTrainingFeedbackRequest(result.id(), "Keep shoulders relaxed"));
		SessionTrainingFeedbackResponse loaded = feedback.get(coach.getId(), sessionId, member.getId())
			.orElseThrow();

		assertThat(updated.classSessionId()).isEqualTo(sessionId);
		assertThat(loaded.id()).isEqualTo(created.id());
		assertThat(loaded.content()).isEqualTo("Keep shoulders relaxed");
		assertThat(updated.trainingResultId()).isEqualTo(result.id());
		assertThat(jdbc.queryForObject(
			"select coach_assignment_id is null from feedback where id = ?", Boolean.class, created.id()))
			.isTrue();
		assertThat(jdbc.queryForObject(
			"select training_plan_version_id is null from feedback where id = ?", Boolean.class, created.id()))
			.isTrue();
		assertThat(jdbc.queryForObject(
			"select content from feedback where id = ?", String.class, created.id()))
			.isEqualTo("Keep shoulders relaxed");
		assertThat(jdbc.queryForObject(
			"select count(*) from audit_events where target_type = 'TRAINING_FEEDBACK' and target_id = ?",
			Long.class, created.id())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("""
			select before_data ->> 'content' from audit_events
			where action = 'TRAINING_FEEDBACK_UPDATED' and target_id = ?
			""", String.class, created.id())).isEqualTo("Good alignment");

		assertThatThrownBy(() -> feedback.update(otherCoach.getId(), sessionId, member.getId(), created.id(),
			new SessionTrainingFeedbackRequest(result.id(), "Denied")))
			.isInstanceOfSatisfying(TrainingFeedbackException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("SESSION_NOT_FOUND"));
	}

	private UUID createBookedYogaSession(UUID managerId, UUID coachId, UUID memberId) {
		UUID disciplineId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Instant startsAt = Instant.parse("2029-01-01T00:00:00Z");
		Instant endsAt = Instant.parse("2031-01-01T00:00:00Z");

		jdbc.update("insert into member_profiles(account_id) values (?)", memberId);
		jdbc.update("insert into disciplines(id, name, status) values (?, ?, 'ACTIVE')",
			disciplineId, "Yoga " + disciplineId);
		jdbc.update("insert into sport_classes(id, discipline_id, name, class_type, status) "
			+ "values (?, ?, ?, 'YOGA', 'ACTIVE')", classId, disciplineId, "Vinyasa " + classId);
		jdbc.update("insert into rooms(id, name, capacity, status) values (?, ?, 20, 'ACTIVE')",
			roomId, "Studio " + roomId);
		jdbc.update("insert into class_sessions(id, sport_class_id, teaching_coach_account_id, room_id, "
			+ "start_time, end_time, capacity, status, created_by_account_id) "
			+ "values (?, ?, ?, ?, '2030-01-01T02:00:00Z', '2030-01-01T03:00:00Z', 12, 'SCHEDULED', ?)",
			sessionId, classId, coachId, roomId, managerId);
		jdbc.update("insert into membership_offers(id, plan_code, name, description, price_amount, "
			+ "duration_days, status, created_by_account_id) "
			+ "values (?, 'PLUS', ?, 'US49 integration offer', 1000, 365, 'ACTIVE', ?)",
			offerId, "US49 Plus " + offerId, managerId);
		jdbc.update("insert into membership_orders(id, order_number, member_account_id, created_by_account_id, "
			+ "offer_id, offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, "
			+ "duration_days_snapshot, payment_method, status, paid_at) "
			+ "values (?, ?, ?, ?, ?, 'US49 Plus', 'PLUS', 1000, 'VND', 365, 'CASH', 'PAID', ?)",
			orderId, "US49-" + orderId.toString().replace("-", ""), memberId, memberId, offerId,
			Timestamp.from(startsAt));
		jdbc.update("insert into payments(id, order_id, method, status, amount, currency_code, "
			+ "processed_by_account_id, paid_at) values (?, ?, 'CASH', 'PAID', 1000, 'VND', ?, ?)",
			UUID.randomUUID(), orderId, managerId, Timestamp.from(startsAt));
		jdbc.update("insert into memberships(id, member_account_id, order_id, offer_id, plan_code_snapshot, "
			+ "offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, "
			+ "status, starts_at, ends_at) values (?, ?, ?, ?, 'PLUS', 'US49 Plus', 1000, 'VND', 365, "
			+ "'ACTIVE', ?, ?)", membershipId, memberId, orderId, offerId, Timestamp.from(startsAt),
			Timestamp.from(endsAt));
		jdbc.update("insert into bookings(id, class_session_id, member_account_id, membership_id, status, "
			+ "booked_by_account_id) values (?, ?, ?, ?, 'BOOKED', ?)", UUID.randomUUID(), sessionId,
			memberId, membershipId, memberId);
		return sessionId;
	}

	private Account createAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		String unique = id.toString().substring(0, 8);
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name, "09" + unique,
			unique + "@example.test", LocalDate.of(1990, 1, 1), "{noop}secret"));
	}
}
