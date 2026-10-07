package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "scms.session-lifecycle-delay-ms=86400000")
@ActiveProfiles("test")
@Import(SessionLifecycleIntegrationTests.FixedClockConfiguration.class)
class SessionLifecycleIntegrationTests {

	private static final Instant NOW = Instant.parse("2026-10-08T03:00:00Z");
	private static final AtomicInteger PHONE = new AtomicInteger(38_000_000);

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired SessionLifecycleService lifecycle;
	@Autowired AttendanceService attendanceService;
	@Autowired AttendanceRepository attendance;
	@Autowired ClassSessionRepository sessions;
	@Autowired BookingRepository bookings;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate jdbc;

	@Test
	void exactStartAndDowntimeCatchUpBackfillOnlyEligibleBookingsAndStayIdempotent() {
		Account manager = account(AccountRole.MANAGER, "Manager");
		Account coach = account(AccountRole.COACH, "Coach");
		MemberProfile bookedMember = member("Booked");
		MemberProfile cancelledMember = member("Cancelled booking");
		MemberProfile cancelledSessionMember = member("Cancelled session");
		MemberProfile downtimeMember = member("Downtime");
		Resources resources = resources(manager, coach);
		ClassSession exact = session(resources, manager, NOW, NOW.plusSeconds(3600));
		Booking exactBooking = booking(exact, bookedMember, manager);
		ClassSession cancelledSession = session(resources, manager, NOW, NOW.plusSeconds(3600));
		cancelledSession.cancel(NOW.minusSeconds(1), "cancelled");
		sessions.saveAndFlush(cancelledSession);
		booking(cancelledSession, cancelledSessionMember, manager);
		Booking cancelledBooking = booking(exact, cancelledMember, manager);
		cancelledBooking.cancel(manager.getId(), "MEMBER", NOW.minusSeconds(1));
		bookings.saveAndFlush(cancelledBooking);
		ClassSession downtime = session(resources(manager, account(AccountRole.COACH, "Downtime coach")), manager,
			NOW.minusSeconds(7200), NOW.minusSeconds(3600));
		Booking downtimeBooking = booking(downtime, downtimeMember, manager);

		lifecycle.catchUp(NOW);
		lifecycle.catchUp(NOW);

		assertThat(status(exact)).isEqualTo(ClassSessionStatus.IN_PROGRESS);
		assertThat(status(downtime)).isEqualTo(ClassSessionStatus.COMPLETED);
		assertThat(status(cancelledSession)).isEqualTo(ClassSessionStatus.CANCELLED);
		assertThat(attendance.findByBookingId(exactBooking.getId())).get().satisfies(row -> {
			assertThat(row.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
			assertThat(row.getRecordedAt()).isEqualTo(exact.getStartTime());
		});
		assertThat(attendance.findByBookingId(downtimeBooking.getId())).isPresent();
		assertThat(attendance.findByBookingId(cancelledBooking.getId())).isEmpty();
		assertThat(jdbc.queryForObject("select count(*) from attendance where booking_id in (?, ?)", Long.class,
			exactBooking.getId(), downtimeBooking.getId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select recording_source from attendance where booking_id=?", String.class,
			exactBooking.getId())).isEqualTo("SYSTEM");
	}

	@Test
	void concurrentWorkersCreateOneAttendance() throws Exception {
		Account manager = account(AccountRole.MANAGER, "Concurrent manager");
		ClassSession session = session(resources(manager, account(AccountRole.COACH, "Concurrent coach")), manager,
			NOW, NOW.plusSeconds(3600));
		Booking booking = booking(session, member("Concurrent member"), manager);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var work = (java.util.concurrent.Callable<Void>) () -> {
				ready.countDown();
				start.await();
				lifecycle.catchUp(NOW);
				return null;
			};
			var first = executor.submit(work);
			var second = executor.submit(work);
			ready.await();
			start.countDown();
			first.get();
			second.get();
		}
		assertThat(jdbc.queryForObject("select count(*) from attendance where booking_id=?", Long.class,
			booking.getId())).isOne();
	}

	@Test
	void attendanceUsesHalfOpenWindowAndMemberVisitOverlap() {
		Account manager = account(AccountRole.MANAGER, "Attendance manager");
		Account coach = account(AccountRole.COACH, "Attendance coach");
		MemberProfile member = member("Attendance member");
		ClassSession session = session(resources(manager, coach), manager, NOW, NOW.plusSeconds(3600));
		Booking booking = booking(session, member, manager);
		lifecycle.catchUp(NOW);
		Attendance row = attendance.findByBookingId(booking.getId()).orElseThrow();
		UUID visitId = visit(member.getAccountId(), booking.getMembershipId(), manager.getId(),
			NOW.minusSeconds(600), NOW.plusSeconds(600));

		AttendanceResponse response = attendanceService.update(coach.getId(), session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.PRESENT));

		assertThat(response.memberId()).isEqualTo(member.getAccountId());
		assertThat(response.centerVisitId()).isEqualTo(visitId);
		jdbc.update("update class_sessions set start_time=?, end_time=? where id=?",
			java.sql.Timestamp.from(NOW.minusSeconds(5400)), java.sql.Timestamp.from(NOW.minusSeconds(1800)),
			session.getId());
		assertThatThrownBy(() -> attendanceService.update(coach.getId(), session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.ABSENT)))
			.isInstanceOf(AttendanceException.class).extracting("code")
			.isEqualTo("ATTENDANCE_WINDOW_CLOSED");
	}

	@Test
	void presentRejectsWrongMemberFutureAndNonOverlappingVisits() {
		Account manager = account(AccountRole.MANAGER, "Reject manager");
		Account coach = account(AccountRole.COACH, "Reject coach");
		MemberProfile member = member("Target member");
		MemberProfile other = member("Other member");
		ClassSession session = session(resources(manager, coach), manager, NOW.minusSeconds(900),
			NOW.plusSeconds(900));
		Booking booking = booking(session, member, manager);
		lifecycle.catchUp(NOW);
		Attendance row = attendance.findByBookingId(booking.getId()).orElseThrow();
		visit(other.getAccountId(), membership(other.getAccountId(), manager.getId()), manager.getId(),
			NOW.minusSeconds(300), null);
		visit(member.getAccountId(), booking.getMembershipId(), manager.getId(), NOW.plusSeconds(60), null);
		visit(member.getAccountId(), booking.getMembershipId(), manager.getId(), NOW.minusSeconds(1800),
			NOW.minusSeconds(901));

		assertThatThrownBy(() -> attendanceService.update(coach.getId(), session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.PRESENT)))
			.isInstanceOf(AttendanceException.class).extracting("code").isEqualTo("CENTER_VISIT_REQUIRED");
	}

	private Booking booking(ClassSession session, MemberProfile member, Account actor) {
		UUID membershipId = membership(member.getAccountId(), actor.getId());
		return bookings.saveAndFlush(new Booking(UUID.randomUUID(), session.getId(), member.getAccountId(),
			membershipId, actor.getId(), NOW.minusSeconds(3600)));
	}

	private ClassSession session(Resources resources, Account manager, Instant start, Instant end) {
		return sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), resources.sportClass(), null,
			resources.coach(), resources.room(), start, end, 20, manager.getId()));
	}

	private Resources resources(Account manager, Account coach) {
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Discipline " + UUID.randomUUID(), null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Class " + UUID.randomUUID(), SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Room " + UUID.randomUUID(), 30,
			RoomStatus.ACTIVE));
		return new Resources(sportClass, coach, room);
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + PHONE.getAndIncrement(), id + "@pr2.test", LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private MemberProfile member(String name) {
		return profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER, name)));
	}

	private UUID membership(UUID memberId, UUID actorId) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id,plan_code,name,price_amount,duration_days,status,created_by_account_id) values(?,'PLUS',?,1000,30,'ACTIVE',?)",
			offerId, "Offer " + offerId, actorId);
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at) values(?,?,?, ?,?,'Plus','PLUS',1000,'VND',30,'CASH','PAID',?)",
			orderId, "ORD-" + orderId, memberId, actorId, offerId, java.sql.Timestamp.from(NOW.minusSeconds(7200)));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?, 'PLUS','Plus',1000,'VND',30,'ACTIVE',?,?)",
			membershipId, memberId, orderId, offerId, java.sql.Timestamp.from(NOW.minusSeconds(86400)),
			java.sql.Timestamp.from(NOW.plusSeconds(86400)));
		return membershipId;
	}

	private UUID visit(UUID memberId, UUID membershipId, UUID actorId, Instant in, Instant out) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at,checked_out_at,checked_out_by_account_id,checkout_source) values(?,?,?,?,?,?,?,?)",
			id, memberId, membershipId, actorId, java.sql.Timestamp.from(in),
			out == null ? null : java.sql.Timestamp.from(out), out == null ? null : actorId,
			out == null ? null : "RECEPTIONIST");
		return id;
	}

	private ClassSessionStatus status(ClassSession session) {
		return ClassSessionStatus.valueOf(jdbc.queryForObject("select status from class_sessions where id=?",
			String.class, session.getId()));
	}

	private record Resources(SportClass sportClass, Account coach, Room room) { }

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}
}
