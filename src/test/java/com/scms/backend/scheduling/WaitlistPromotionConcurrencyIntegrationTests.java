package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "scms.waitlist-promotion-delay-ms=3600000")
@ActiveProfiles("test")
class WaitlistPromotionConcurrencyIntegrationTests {
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
	@Autowired JdbcTemplate db;
	@Autowired WaitlistPromotionCoordinator coordinator;
	@Autowired ClassSessionRepository sessions;
	@Autowired BookingService bookingService;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired Clock clock;

	@Test
	void failedPromotionIsRetriedFromDurableStateAfterSchedulerRestart() {
		Fixture fixture = fixture(1);
		UUID waiter = account("MEMBER");
		membership(waiter, fixture.manager(), fixture.start());
		UUID entry = waitlist(fixture.session(), waiter, fixture.start().minusSeconds(60));
		db.execute("""
			create function fail_gap015_booking_insert() returns trigger language plpgsql as $$
			begin raise exception 'forced promotion failure'; end $$
			""");
		db.execute("create trigger fail_gap015_booking_insert before insert on bookings "
			+ "for each row execute function fail_gap015_booking_insert()");
		try {
			coordinator.promote(fixture.session());
		}
		finally {
			db.execute("drop trigger fail_gap015_booking_insert on bookings");
			db.execute("drop function fail_gap015_booking_insert()");
		}
		assertThat(waitlistStatus(entry)).isEqualTo("WAITING");
		assertThat(bookedCount(fixture.session())).isZero();
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='WAITLIST_PROMOTED'",
			Long.class, entry)).isZero();
		assertThat(db.queryForObject("select count(*) from notification_outbox where event_key=?", Long.class,
			"waitlist-promoted:" + entry)).isZero();

		new WaitlistPromotionScheduler(sessions, coordinator, clock).promoteAvailable();

		assertThat(waitlistStatus(entry)).isEqualTo("PROMOTED");
		assertThat(bookedCount(fixture.session())).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='WAITLIST_PROMOTED'",
			Long.class, entry)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where event_key=?", Long.class,
			"waitlist-promoted:" + entry)).isOne();
	}

	@Test
	void twoWorkersCannotOverbookOrDuplicatePromotionSideEffects() throws Exception {
		Fixture fixture = fixture(1);
		UUID first = account("MEMBER");
		UUID second = account("MEMBER");
		membership(first, fixture.manager(), fixture.start());
		membership(second, fixture.manager(), fixture.start());
		waitlist(fixture.session(), first, fixture.start().minusSeconds(120));
		waitlist(fixture.session(), second, fixture.start().minusSeconds(60));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var work = (java.util.concurrent.Callable<Void>) () -> {
				ready.countDown();
				await(start);
				coordinator.promote(fixture.session());
				return null;
			};
			var firstWorker = executor.submit(work);
			var secondWorker = executor.submit(work);
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			firstWorker.get(10, TimeUnit.SECONDS);
			secondWorker.get(10, TimeUnit.SECONDS);
		}

		assertThat(bookedCount(fixture.session())).isOne();
		assertThat(db.queryForObject("select count(*) from session_waitlists where class_session_id=? and status='PROMOTED'",
			Long.class, fixture.session())).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where action='WAITLIST_PROMOTED' "
			+ "and target_id in (select id from session_waitlists where class_session_id=?)", Long.class,
			fixture.session())).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where notification_type='WAITLIST_PROMOTED' "
			+ "and target_id in (select id from bookings where class_session_id=?)", Long.class,
			fixture.session())).isOne();
	}

	@Test
	void directBookingAndPromotionSerializeOnSessionCapacity() throws Exception {
		Fixture fixture = fixture(1);
		UUID waiter = account("MEMBER");
		UUID directMember = account("MEMBER");
		membership(waiter, fixture.manager(), fixture.start());
		membership(directMember, fixture.manager(), fixture.start());
		UUID entry = waitlist(fixture.session(), waiter, fixture.start().minusSeconds(60));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		boolean directBooked;
		try (var executor = Executors.newFixedThreadPool(2)) {
			var promotion = executor.submit(() -> {
				ready.countDown();
				await(start);
				coordinator.promote(fixture.session());
				return null;
			});
			var direct = executor.submit(() -> {
				ready.countDown();
				await(start);
				try {
					bookingService.book(directMember, fixture.session());
					return true;
				}
				catch (BookingException exception) {
					assertThat(exception.getCode()).isEqualTo("SESSION_FULL");
					return false;
				}
			});
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			promotion.get(10, TimeUnit.SECONDS);
			directBooked = direct.get(10, TimeUnit.SECONDS);
		}

		assertThat(bookedCount(fixture.session())).isOne();
		assertThat(directBooked).isEqualTo("WAITING".equals(waitlistStatus(entry)));
	}

	@Test
	void busyHeadAccountDefersWholeSessionWithoutWaitingOrBypassing() throws Exception {
		Fixture fixture = fixture(1);
		UUID head = account("MEMBER");
		UUID second = account("MEMBER");
		membership(head, fixture.manager(), fixture.start());
		membership(second, fixture.manager(), fixture.start());
		UUID headEntry = waitlist(fixture.session(), head, fixture.start().minusSeconds(120));
		UUID secondEntry = waitlist(fixture.session(), second, fixture.start().minusSeconds(60));
		CountDownLatch accountLocked = new CountDownLatch(1);
		CountDownLatch releaseAccount = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var lockHolder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
				db.queryForObject("select id from accounts where id=? for update", UUID.class, head);
				accountLocked.countDown();
				await(releaseAccount);
			}));
			assertThat(accountLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var promotion = executor.submit(() -> coordinator.promote(fixture.session()));
			try {
				promotion.get(5, TimeUnit.SECONDS);
				assertThat(waitlistStatus(headEntry)).isEqualTo("WAITING");
				assertThat(waitlistStatus(secondEntry)).isEqualTo("WAITING");
				assertThat(bookedCount(fixture.session())).isZero();
			}
			finally {
				releaseAccount.countDown();
			}
			lockHolder.get(10, TimeUnit.SECONDS);
		}

		coordinator.promote(fixture.session());
		assertThat(waitlistStatus(headEntry)).isEqualTo("PROMOTED");
		assertThat(waitlistStatus(secondEntry)).isEqualTo("WAITING");
	}

	private Fixture fixture(int capacity) {
		Instant start = Instant.now().plusSeconds(7200);
		UUID manager = account("MANAGER");
		UUID coach = account("COACH");
		UUID discipline = UUID.randomUUID();
		UUID sportClass = UUID.randomUUID();
		UUID room = UUID.randomUUID();
		UUID session = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", discipline,
			"GAP015 discipline " + discipline);
		db.update("insert into sport_classes(id,discipline_id,name,class_type,status) values(?,?,?,'GROUP','ACTIVE')",
			sportClass, discipline, "GAP015 class " + sportClass);
		db.update("insert into rooms(id,name,capacity,status) values(?,?,10,'ACTIVE')", room,
			"GAP015 room " + room);
		db.update("insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,capacity,status,created_by_account_id) values(?,?,?,?,?,?,?,'SCHEDULED',?)",
			session, sportClass, coach, room, Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)), capacity,
			manager);
		return new Fixture(manager, session, start);
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE',?,?,?,?,?)",
			id, role, "GAP015 " + role, "09" + id.toString().replace("-", "").substring(0, 9),
			id + "@gap015.test", java.sql.Date.valueOf("1990-01-01"), "hash");
		return id;
	}

	private UUID membership(UUID member, UUID manager, Instant sessionStart) {
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'PLUS',?,?,100000,'VND',30,'ACTIVE',?)",
			offer, "GAP015 offer " + offer, "GAP015", manager);
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,expires_at,paid_at,fulfillment_mode) values(?,?,?, ?,?,'GAP015 offer','PLUS',100000,'VND',30,'CASH','PAID',?,?,'IMMEDIATE')",
			order, "GAP015-" + order.toString().replace("-", ""), member, manager, offer,
			Timestamp.from(sessionStart), Timestamp.from(sessionStart.minusSeconds(7200)));
		db.update("insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at) values(?,?,'CASH','PAID',100000,'VND',?,?)",
			UUID.randomUUID(), order, manager, Timestamp.from(sessionStart.minusSeconds(7200)));
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?, 'PLUS','GAP015 offer',100000,'VND',30,'ACTIVE',?,?)",
			membership, member, order, offer, Timestamp.from(sessionStart.minusSeconds(86400)),
			Timestamp.from(sessionStart.plusSeconds(86400)));
		return membership;
	}

	private UUID waitlist(UUID session, UUID member, Instant createdAt) {
		UUID id = UUID.randomUUID();
		db.update("insert into session_waitlists(id,class_session_id,member_account_id,status,joined_by_account_id,source,created_at,updated_at) values(?,?,?,'WAITING',?,'MEMBER',?,?)",
			id, session, member, member, Timestamp.from(createdAt), Timestamp.from(createdAt));
		return id;
	}

	private long bookedCount(UUID session) {
		return db.queryForObject("select count(*) from bookings where class_session_id=? and status='BOOKED'",
			Long.class, session);
	}

	private String waitlistStatus(UUID id) {
		return db.queryForObject("select status from session_waitlists where id=?", String.class, id);
	}

	private void await(CountDownLatch latch) {
		try {
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}

	private record Fixture(UUID manager, UUID session, Instant start) { }
}
