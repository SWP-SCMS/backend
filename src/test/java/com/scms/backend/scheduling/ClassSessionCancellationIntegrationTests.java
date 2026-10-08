package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class ClassSessionCancellationIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired ClassSessionCancellationService service;
	@Autowired BookingService bookingService;
	@Autowired ClassSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate jdbc;

	@Test
	void memberBookingIsPersistedAuditedNotifiedAndCannotOverbook() {
		Account manager = saveAccount(AccountRole.MANAGER, "Booking Manager");
		Account coach = saveAccount(AccountRole.COACH, "Booking Coach");
		Account member = saveAccount(AccountRole.MEMBER, "Booking Member");
		Account otherMember = saveAccount(AccountRole.MEMBER, "Other Member");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Pilates", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Core",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Booking Studio", 10, RoomStatus.ACTIVE));
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			room, Instant.parse("2030-01-01T04:00:00Z"), Instant.parse("2030-01-01T05:00:00Z"), 1,
			manager.getId()));
		UUID membershipId = insertMembership(manager.getId(), member.getId());
		insertMembership(manager.getId(), otherMember.getId());

		installNotificationFailure();
		try {
			assertThatThrownBy(() -> bookingService.book(member.getId(), session.getId()))
				.hasStackTraceContaining("forced notification failure");
		}
		finally {
			dropNotificationFailure();
		}
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id = ?", Long.class,
			session.getId())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from audit_events where action = 'BOOKING_CREATED' "
			+ "and actor_account_id = ?", Long.class, member.getId())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from notifications where notification_type = 'BOOKING_CREATED' "
			+ "and recipient_account_id = ?", Long.class, member.getId())).isZero();

		BookingResponse response = bookingService.book(member.getId(), session.getId());

		assertThat(response.membershipId()).isEqualTo(membershipId);
		assertThat(jdbc.queryForObject("select count(*) from bookings where id = ? and status = 'BOOKED'",
			Long.class, response.id())).isEqualTo(1L);
		assertThat(jdbc.queryForObject("select count(*) from audit_events where action = 'BOOKING_CREATED' "
			+ "and target_id = ?", Long.class, response.id())).isEqualTo(1L);
		assertThat(jdbc.queryForObject("select count(*) from notifications where event_key = ? and target_id = ?",
			Long.class, "BOOKING_CREATED:" + response.id(), response.id())).isEqualTo(1L);
		assertThatThrownBy(() -> bookingService.book(member.getId(), session.getId()))
			.isInstanceOf(BookingException.class).extracting("code").isEqualTo("BOOKING_DUPLICATE");
		assertThatThrownBy(() -> bookingService.book(otherMember.getId(), session.getId()))
			.isInstanceOf(BookingException.class).extracting("code").isEqualTo("SESSION_FULL");

		installNotificationFailure();
		try {
			assertThatThrownBy(() -> bookingService.cancel(member.getId(), response.id()))
				.hasStackTraceContaining("forced notification failure");
		}
		finally {
			dropNotificationFailure();
		}
		assertThat(jdbc.queryForObject("select status from bookings where id = ?", String.class, response.id()))
			.isEqualTo("BOOKED");
		assertThat(jdbc.queryForObject("select count(*) from audit_events where action = 'BOOKING_CANCELLED' "
			+ "and target_id = ?", Long.class, response.id())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from notifications where event_key = ?", Long.class,
			"BOOKING_CANCELLED:" + response.id())).isZero();

		BookingResponse cancelled = bookingService.cancel(member.getId(), response.id());
		BookingResponse rebooked = bookingService.book(member.getId(), session.getId());

		assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(cancelled.cancellationSource()).isEqualTo("MEMBER");
		assertThat(rebooked.id()).isNotEqualTo(response.id());
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id = ?",
			Long.class, session.getId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select count(*) from audit_events where action = 'BOOKING_CANCELLED' "
			+ "and target_id = ?", Long.class, response.id())).isEqualTo(1L);
		assertThat(jdbc.queryForObject("select count(*) from notifications where event_key = ? and target_id = ?",
			Long.class, "BOOKING_CANCELLED:" + response.id(), response.id())).isEqualTo(1L);
	}

	@Test
	void receptionistBooksAndCancelsForMemberWithCorrectOwnershipAndActor() {
		Account manager = saveAccount(AccountRole.MANAGER, "Reception Booking Manager");
		Account receptionist = saveAccount(AccountRole.RECEPTIONIST, "Reception Booking Receptionist");
		Account coach = saveAccount(AccountRole.COACH, "Reception Booking Coach");
		Account member = saveAccount(AccountRole.MEMBER, "Reception Booking Member");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Boxing", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Fundamentals",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Reception Booking Studio", 10,
			RoomStatus.ACTIVE));
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			room, Instant.parse("2030-02-01T04:00:00Z"), Instant.parse("2030-02-01T05:00:00Z"), 10,
			manager.getId()));
		insertMembership(manager.getId(), member.getId());

		BookingResponse booked = bookingService.bookForMember(receptionist.getId(), member.getId(), session.getId());
		BookingResponse cancelled = bookingService.cancelForMember(receptionist.getId(), member.getId(), booked.id());

		assertThat(booked.memberId()).isEqualTo(member.getId());
		assertThat(booked.bookedBy()).isEqualTo(receptionist.getId());
		assertThat(cancelled.cancelledBy()).isEqualTo(receptionist.getId());
		assertThat(cancelled.cancellationSource()).isEqualTo("RECEPTIONIST");
		assertThat(jdbc.queryForMap("""
			select member_account_id, booked_by_account_id, cancelled_by_account_id, cancellation_source
			from bookings where id = ?
			""", booked.id())).containsEntry("member_account_id", member.getId())
			.containsEntry("booked_by_account_id", receptionist.getId())
			.containsEntry("cancelled_by_account_id", receptionist.getId())
			.containsEntry("cancellation_source", "RECEPTIONIST");
		assertThat(jdbc.queryForObject("""
			select count(*) from audit_events
			where target_id = ? and actor_account_id = ? and action in ('BOOKING_CREATED', 'BOOKING_CANCELLED')
			""", Long.class, booked.id(), receptionist.getId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("""
			select count(*) from notifications where target_id = ? and recipient_account_id = ?
			""", Long.class, booked.id(), member.getId())).isEqualTo(2L);
	}

	@Test
	void cancellationIsAtomicKeepsHistoryAndNotifiesAffectedMemberOnce() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach");
		Account member = saveAccount(AccountRole.MEMBER, "Member");
		Account secondMember = saveAccount(AccountRole.MEMBER, "Second Member");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Yoga", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.YOGA, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE));
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			room, Instant.parse("2030-01-01T02:00:00Z"), Instant.parse("2030-01-01T03:00:00Z"), 12,
			manager.getId()));
		UUID membershipId = insertMembership(manager.getId(), member.getId());
		UUID secondMembershipId = insertMembership(manager.getId(), secondMember.getId());
		UUID bookingId = UUID.randomUUID();
		UUID secondBookingId = UUID.randomUUID();
		jdbc.update("""
			insert into bookings (id, class_session_id, member_account_id, membership_id, status,
				booked_by_account_id)
			values (?, ?, ?, ?, 'BOOKED', ?)
			""", bookingId, session.getId(), member.getId(), membershipId, member.getId());
		jdbc.update("""
			insert into bookings (id, class_session_id, member_account_id, membership_id, status,
				booked_by_account_id)
			values (?, ?, ?, ?, 'BOOKED', ?)
			""", secondBookingId, session.getId(), secondMember.getId(), secondMembershipId, secondMember.getId());
		assertThat(sessions.countBookedBySessionIds(List.of(session.getId()))).singleElement().satisfies(count -> {
			assertThat(count.getSessionId()).isEqualTo(session.getId());
			assertThat(count.getBookedCount()).isEqualTo(2);
		});

		installNotificationFailure();
		try {
			assertThatThrownBy(() -> service.cancel(manager.getId(), session.getId(),
				new ClassSessionCancellationRequest("Coach unavailable")))
				.hasStackTraceContaining("forced notification failure");
		}
		finally {
			dropNotificationFailure();
		}
		assertThat(jdbc.queryForObject("select status from class_sessions where id = ?", String.class,
			session.getId())).isEqualTo("SCHEDULED");
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id = ? and status = 'BOOKED'",
			Long.class, session.getId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select count(*) from audit_events where action = 'SESSION_CANCELLED' "
			+ "and target_id = ?", Long.class, session.getId())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from notifications where notification_type = 'SESSION_CANCELLED' "
			+ "and target_id = ?", Long.class, session.getId())).isZero();

		service.cancel(manager.getId(), session.getId(), new ClassSessionCancellationRequest("Coach unavailable"));

		assertThat(jdbc.queryForObject("select status from class_sessions where id = ?", String.class,
			session.getId())).isEqualTo("CANCELLED");
		assertThat(jdbc.queryForMap("""
			select status, cancellation_source, cancelled_by_account_id from bookings where id = ?
			""", bookingId)).containsEntry("status", "CANCELLED")
			.containsEntry("cancellation_source", "SESSION_CANCELLED")
			.containsEntry("cancelled_by_account_id", manager.getId());
		assertThat(jdbc.queryForObject("""
			select count(*) from audit_events
			where action = 'SESSION_CANCELLED' and target_id = ? and actor_account_id = ?
			""", Long.class, session.getId(), manager.getId())).isEqualTo(1L);
		assertThat(jdbc.queryForObject("""
			select count(*) from notifications
			where notification_type = 'SESSION_CANCELLED' and target_id = ?
			""", Long.class, session.getId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("""
			select count(*) from notifications
			where event_key = ? and recipient_account_id = ?
			""", Long.class, "SESSION_CANCELLED:" + session.getId() + ":" + member.getId(), member.getId())).isOne();
		assertThat(jdbc.queryForObject("""
			select count(*) from notifications
			where event_key = ? and recipient_account_id = ?
			""", Long.class, "SESSION_CANCELLED:" + session.getId() + ":" + secondMember.getId(),
			secondMember.getId())).isOne();

		assertThatThrownBy(() -> service.cancel(manager.getId(), session.getId(),
			new ClassSessionCancellationRequest("again")))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_NOT_SCHEDULED");
		assertThat(jdbc.queryForObject("select count(*) from bookings where id = ?", Long.class, bookingId))
			.isEqualTo(1L);
	}

	private UUID insertMembership(UUID managerId, UUID memberId) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, 'PLUS', 'Plus', 'Plus membership', 12000, 3650, 'ACTIVE', ?)
			""", offerId, managerId);
		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Plus', 'PLUS', 12000, 'VND', 3650, 'CASH', 'PAID', ?, current_timestamp)
			""", orderId, "ORD-" + orderId, memberId, managerId, offerId,
			java.sql.Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));
		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 12000, 'VND', ?, current_timestamp)
			""", paymentId, orderId, managerId);
		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, 'PLUS', 'Plus', 12000, 'VND', 3650, 'ACTIVE', current_timestamp, ?)
			""", membershipId, memberId, orderId, offerId,
			java.sql.Timestamp.from(Instant.parse("2035-01-01T00:00:00Z")));
		return membershipId;
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private void installNotificationFailure() {
		jdbc.execute("create function fail_notification() returns trigger language plpgsql as $$ begin "
			+ "raise exception 'forced notification failure'; end; $$");
		jdbc.execute("create trigger fail_notification before insert on notifications "
			+ "for each row execute function fail_notification()");
	}

	private void dropNotificationFailure() {
		jdbc.execute("drop trigger fail_notification on notifications");
		jdbc.execute("drop function fail_notification()");
	}
}
