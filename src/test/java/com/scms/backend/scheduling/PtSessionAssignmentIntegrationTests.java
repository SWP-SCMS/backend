package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.training.CoachAssignment;
import com.scms.backend.training.CoachAssignmentRepository;
import com.scms.backend.training.CoachAssignmentStatus;
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
class PtSessionAssignmentIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired ClassSessionService sessionService;
	@Autowired BookingService bookingService;
	@Autowired ClassSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired CoachAssignmentRepository assignments;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate jdbc;

	@Test
	void rejectsIncompatibleBookingAndWaitlistThenSucceedsAfterAssignmentsAreFixed() {
		Account manager = account(AccountRole.MANAGER, "PT Assignment Manager");
		Account oldCoach = account(AccountRole.COACH, "PT Old Coach");
		Account newCoach = account(AccountRole.COACH, "PT New Coach");
		MemberProfile bookedMember = member("PT Booked Member");
		MemberProfile waitlistedMember = member("PT Waitlisted Member");
		ClassSession session = ptSession(manager, oldCoach, "PT Compatibility");
		UUID bookingId = booking(session, bookedMember.getAccountId(), membership(manager, bookedMember.getAccountId()));
		UUID waitlistId = waitlist(session, waitlistedMember.getAccountId());

		assertThatThrownBy(() -> sessionService.updateAssignment(manager.getId(), session.getId(),
			new ClassSessionAssignmentRequest(newCoach.getId(), null)))
			.isInstanceOfSatisfying(PtSessionAssignmentException.class, error -> {
				assertThat(error.getAffectedBookings()).containsExactly(new PtSessionAffectedBooking(bookingId,
					bookedMember.getAccountId(), bookedMember.getMemberCode(), "PT Booked Member"));
				assertThat(error.getAffectedWaitlists()).containsExactly(new PtSessionAffectedWaitlist(waitlistId,
					waitlistedMember.getAccountId(), waitlistedMember.getMemberCode(), "PT Waitlisted Member"));
			});
		assertThat(coachId(session.getId())).isEqualTo(oldCoach.getId());

		assign(manager, bookedMember.getAccountId(), newCoach.getId());
		assign(manager, waitlistedMember.getAccountId(), newCoach.getId());

		assertThat(sessionService.updateAssignment(manager.getId(), session.getId(),
			new ClassSessionAssignmentRequest(newCoach.getId(), null)).coachId()).isEqualTo(newCoach.getId());
		assertThat(coachId(session.getId())).isEqualTo(newCoach.getId());
		assertThat(jdbc.queryForObject("select status from bookings where id=?", String.class, bookingId))
			.isEqualTo("BOOKED");
		assertThat(jdbc.queryForObject("select status from session_waitlists where id=?", String.class, waitlistId))
			.isEqualTo("WAITING");
	}

	@Test
	void concurrentBookingAndCoachChangeCannotCommitAnIncompatibleFinalState() throws Exception {
		Account manager = account(AccountRole.MANAGER, "PT Race Manager");
		Account oldCoach = account(AccountRole.COACH, "PT Race Old Coach");
		Account newCoach = account(AccountRole.COACH, "PT Race New Coach");
		MemberProfile member = member("PT Race Member");
		ClassSession session = ptSession(manager, oldCoach, "PT Race");
		membership(manager, member.getAccountId());
		assign(manager, member.getAccountId(), oldCoach.getId());
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch go = new CountDownLatch(1);

		try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
			var assignment = executor.submit(() -> race(ready, go, () -> sessionService.updateAssignment(
				manager.getId(), session.getId(), new ClassSessionAssignmentRequest(newCoach.getId(), null))));
			var booking = executor.submit(() -> race(ready, go,
				() -> bookingService.book(member.getAccountId(), session.getId())));
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			go.countDown();
			Object assignmentResult = assignment.get(20, TimeUnit.SECONDS);
			Object bookingResult = booking.get(20, TimeUnit.SECONDS);
			boolean assignmentSucceeded = assignmentResult instanceof ClassSessionDetailResponse;
			boolean bookingSucceeded = bookingResult instanceof BookingResponse;
			assertThat(List.of(assignmentSucceeded, bookingSucceeded)).containsExactlyInAnyOrder(true, false);
			if (assignmentSucceeded) {
				assertThat(bookingResult).isInstanceOf(BookingException.class);
			}
			else {
				assertThat(assignmentResult).isInstanceOf(PtSessionAssignmentException.class);
			}
		}

		UUID finalCoachId = coachId(session.getId());
		long booked = jdbc.queryForObject(
			"select count(*) from bookings where class_session_id=? and status='BOOKED'", Long.class,
			session.getId());
		assertThat((finalCoachId.equals(newCoach.getId()) && booked == 0)
			|| (finalCoachId.equals(oldCoach.getId()) && booked == 1)).isTrue();
	}

	private Object race(CountDownLatch ready, CountDownLatch go, ThrowingAction action) throws InterruptedException {
		ready.countDown();
		go.await();
		try {
			return action.run();
		}
		catch (RuntimeException error) {
			return error;
		}
	}

	private ClassSession ptSession(Account manager, Account coach, String suffix) {
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), suffix, null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, suffix,
			SportClassType.PT_1_1, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), suffix + " Room", 10, RoomStatus.ACTIVE));
		return sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach, room,
			Instant.parse("2035-01-01T02:00:00Z"), Instant.parse("2035-01-01T03:00:00Z"), 1,
			manager.getId()));
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private MemberProfile member(String name) {
		return profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER, name)));
	}

	private void assign(Account manager, UUID memberId, UUID coachId) {
		assignments.saveAndFlush(new CoachAssignment(UUID.randomUUID(), memberId, coachId, manager.getId(),
			CoachAssignmentStatus.ACTIVE, Instant.parse("2030-01-01T00:00:00Z"),
			Instant.parse("2030-01-01T00:00:00Z")));
	}

	private UUID booking(ClassSession session, UUID memberId, UUID membershipId) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
			insert into bookings (id, class_session_id, member_account_id, membership_id, status,
				booked_by_account_id)
			values (?, ?, ?, ?, 'BOOKED', ?)
			""", id, session.getId(), memberId, membershipId, memberId);
		return id;
	}

	private UUID waitlist(ClassSession session, UUID memberId) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
			insert into session_waitlists (id, class_session_id, member_account_id, status,
				joined_by_account_id, source, created_at, updated_at)
			values (?, ?, ?, 'WAITING', ?, 'MEMBER', current_timestamp, current_timestamp)
			""", id, session.getId(), memberId, memberId);
		return id;
	}

	private UUID membership(Account manager, UUID memberId) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, 'PLUS', ?, 'Plus membership', 12000, 3650, 'ACTIVE', ?)
			""", offerId, "Plus " + offerId, manager.getId());
		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Plus', 'PLUS', 12000, 'VND', 3650, 'CASH', 'PAID', ?, current_timestamp)
			""", orderId, "ORD-" + orderId, memberId, manager.getId(), offerId,
			Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));
		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 12000, 'VND', ?, current_timestamp)
			""", paymentId, orderId, manager.getId());
		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, 'PLUS', 'Plus', 12000, 'VND', 3650, 'ACTIVE', current_timestamp, ?)
			""", membershipId, memberId, orderId, offerId, Timestamp.from(Instant.parse("2040-01-01T00:00:00Z")));
		return membershipId;
	}

	private UUID coachId(UUID sessionId) {
		return jdbc.queryForObject("select teaching_coach_account_id from class_sessions where id=?", UUID.class,
			sessionId);
	}

	@FunctionalInterface
	private interface ThrowingAction {
		Object run();
	}
}
