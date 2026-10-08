package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
class OperationalIntegrityIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired AccountRepository accounts;
	@Autowired MemberStatusService memberStatuses;
	@Autowired StaffStatusService staffStatuses;
	@Autowired JdbcTemplate db;

	@Test
	void suspensionIncrementsBookingVersionAndRejectsStaleVersionWrite() {
		Account manager = account(AccountRole.MANAGER);
		Account coach = account(AccountRole.COACH);
		Account member = account(AccountRole.MEMBER);
		UUID bookingId = futureBooking(manager.getId(), coach.getId(), member.getId());
		long staleVersion = db.queryForObject("select version from bookings where id=?", Long.class, bookingId);

		MemberStatusChangeResponse response = memberStatuses.changeStatus(manager.getId(), member.getId(),
			new MemberStatusChangeRequest(AccountStatus.SUSPENDED, "Operational integrity test"));

		assertThat(response.cancelledFutureBookings()).isOne();
		assertThat(db.queryForObject("select version from bookings where id=?", Long.class, bookingId))
			.isEqualTo(staleVersion + 1);
		int staleUpdate = db.update("""
			update bookings set status='BOOKED',cancelled_by_account_id=null,cancellation_source=null,
				cancelled_at=null,version=version+1 where id=? and version=?
			""", bookingId, staleVersion);
		assertThat(staleUpdate).isZero();
		assertThat(db.queryForObject("select status from bookings where id=?", String.class, bookingId))
			.isEqualTo("CANCELLED");
	}

	@Test
	void twoManagersCannotDeactivateEachOtherToZeroActiveManagers() throws Exception {
		db.update("update accounts set status='INACTIVE' where role='MANAGER' and status='ACTIVE'");
		Account first = account(AccountRole.MANAGER);
		Account second = account(AccountRole.MANAGER);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var firstAttempt = executor.submit(() -> deactivateTogether(first.getId(), second.getId(), ready, start));
			var secondAttempt = executor.submit(() -> deactivateTogether(second.getId(), first.getId(), ready, start));
			ready.await();
			start.countDown();
			StaffStatusConflictException firstFailure = firstAttempt.get();
			StaffStatusConflictException secondFailure = secondAttempt.get();
			assertThat(firstFailure == null ^ secondFailure == null).isTrue();
			assertThat(firstFailure != null ? firstFailure : secondFailure)
				.hasMessage("The last active Manager cannot be deactivated");
		}

		assertThat(db.queryForObject("select count(*) from accounts where role='MANAGER' and status='ACTIVE'",
			Long.class)).isOne();
	}

	private StaffStatusConflictException deactivateTogether(UUID actorId, UUID targetId, CountDownLatch ready,
			CountDownLatch start)
			throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			staffStatuses.deactivate(actorId, targetId,
				new StaffStatusChangeRequest(AccountStatus.INACTIVE, "Concurrent manager test"));
			return null;
		}
		catch (StaffStatusConflictException exception) {
			return exception;
		}
	}

	private UUID futureBooking(UUID managerId, UUID coachId, UUID memberId) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID disciplineId = UUID.randomUUID();
		UUID sportClassId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		Instant now = Instant.now();
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,duration_days,status,created_by_account_id)
			values(?,'PLUS',?,'Operational integrity',12000,30,'ACTIVE',?)
			""", offerId, "Plus " + offerId, managerId);
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				payment_method,status,paid_at)
			values(?,?,?,?,?,'Plus','PLUS',12000,'VND',30,'CASH','PAID',?)
			""", orderId, "ORD-" + orderId.toString().replace("-", "").toUpperCase(), memberId, managerId, offerId,
			Timestamp.from(now));
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,provider_reference,paid_at,processed_by_account_id)
			values(?,?,'CASH','PAID',12000,'VND',?,?,?)
			""", UUID.randomUUID(), orderId, "CASH-" + orderId, Timestamp.from(now), managerId);
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,
				price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at)
			values(?,?,?,?,'PLUS','Plus',12000,'VND',30,'ACTIVE',?,?)
			""", membershipId, memberId, orderId, offerId, Timestamp.from(now.minusSeconds(60)),
			Timestamp.from(now.plusSeconds(86400 * 30L)));
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')",
			disciplineId, "Discipline " + disciplineId);
		db.update("""
			insert into sport_classes(id,discipline_id,name,class_type,status) values(?,?,?,'GROUP','ACTIVE')
			""", sportClassId, disciplineId, "Class " + sportClassId);
		db.update("insert into rooms(id,name,capacity,status) values(?,?,10,'ACTIVE')",
			roomId, "Room " + roomId);
		db.update("""
			insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,
				capacity,status,created_by_account_id)
			values(?,?,?,?,?,?,10,'SCHEDULED',?)
			""", sessionId, sportClassId, coachId, roomId, Timestamp.from(now.plusSeconds(86400)),
			Timestamp.from(now.plusSeconds(90000)), managerId);
		db.update("""
			insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id)
			values(?,?,?,?,'BOOKED',?)
			""", bookingId, sessionId, memberId, membershipId, memberId);
		return bookingId;
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " Integrity",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "unused"));
	}
}
