package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

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
@SpringBootTest
@ActiveProfiles("test")
class MembershipFreezeExecutionIntegrationTests {
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
	@Autowired JdbcTemplate jdbc;
	@Autowired MembershipFreezeExecutionService execution;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	void databaseInstallsTheActiveFreezeBookingAuthority() {
		Boolean installed = jdbc.queryForObject("""
			select exists (select 1 from pg_trigger where tgname = 'trg_bookings_active_membership_freeze' and not tgisinternal)
			""", Boolean.class);
		assertThat(installed).isTrue();
	}

	@Test
	void databaseAcceptsWithdrawnFreezeRequestsAndRejectsUnknownStatuses() {
		Instant now = Instant.now();
		UUID manager = account("MANAGER");
		UUID member = account("MEMBER");
		UUID membership = membership(member, manager, now);
		UUID request = UUID.randomUUID();
		LocalDate start = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
		jdbc.update("insert into membership_freeze_requests(id,membership_id,member_account_id,requester_account_id,start_date,end_date,reason,status) values(?,?,?,?,?,?,'withdrawal','PENDING')",
			request, membership, member, member, java.sql.Date.valueOf(start), java.sql.Date.valueOf(start.plusDays(2)));

		jdbc.update("update membership_freeze_requests set status='WITHDRAWN' where id=?", request);

		assertThat(jdbc.queryForObject("select status from membership_freeze_requests where id=?", String.class, request)).isEqualTo("WITHDRAWN");
		assertThatThrownBy(() -> jdbc.update("update membership_freeze_requests set status='UNKNOWN' where id=?", request))
			.hasStackTraceContaining("chk_membership_freeze_request_status");
	}

	@Test
	void activeFreezeMakesDatabaseRejectDirectBookingInsert() {
		Instant now = Instant.now();
		UUID manager = account("MANAGER");
		UUID member = account("MEMBER");
		UUID coach = account("COACH");
		UUID membership = membership(member, manager, now);
		UUID request = UUID.randomUUID();
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
		jdbc.update("insert into membership_freeze_requests(id,membership_id,member_account_id,requester_account_id,start_date,end_date,reason,status) values(?,?,?,?,?,?,'integration','ACTIVE')",
			request, membership, member, member, java.sql.Date.valueOf(today.minusDays(1)), java.sql.Date.valueOf(today.plusDays(1)));
		jdbc.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,actual_start_date,status) values(?,?,?,?,?,?,'ACTIVE')",
			UUID.randomUUID(), request, membership, java.sql.Date.valueOf(today.minusDays(1)), java.sql.Date.valueOf(today.plusDays(1)), java.sql.Date.valueOf(today.minusDays(1)));

		assertThatThrownBy(() -> jdbc.update("insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id) values(?,?,?,?,'BOOKED',?)",
			UUID.randomUUID(), futureSession(manager, coach, now.plusSeconds(3600)), member, membership, member))
			.hasStackTraceContaining("MEMBERSHIP_FROZEN");
	}

	@Test
	void activatingFreezePromotesEligibleWaitlistMember() {
		Instant now = Instant.now();
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
		UUID manager = account("MANAGER");
		UUID member = account("MEMBER");
		UUID waitingMember = account("MEMBER");
		UUID coach = account("COACH");
		UUID membership = membership(member, manager, now);
		UUID waitingMembership = membership(waitingMember, manager, now);
		UUID session = futureSession(manager, coach, now.plusSeconds(3600));
		UUID booking = UUID.randomUUID();
		UUID waitlist = UUID.randomUUID();
		UUID request = UUID.randomUUID();
		jdbc.update("update class_sessions set capacity=1 where id=?", session);
		jdbc.update("insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id) values(?,?,?,?,'BOOKED',?)",
			booking, session, member, membership, member);
		jdbc.update("insert into session_waitlists(id,class_session_id,member_account_id,status,joined_by_account_id,source,created_at,updated_at) values(?,?,?,'WAITING',?,'MEMBER',?,?)",
			waitlist, session, waitingMember, waitingMember, Timestamp.from(now.minusSeconds(60)), Timestamp.from(now.minusSeconds(60)));
		jdbc.update("insert into membership_freeze_requests(id,membership_id,member_account_id,requester_account_id,start_date,end_date,reason,status) values(?,?,?,?,?,?,'integration','SCHEDULED')",
			request, membership, member, member, java.sql.Date.valueOf(today), java.sql.Date.valueOf(today.plusDays(2)));
		jdbc.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,status) values(?,?,?,?,?,'SCHEDULED')",
			UUID.randomUUID(), request, membership, java.sql.Date.valueOf(today), java.sql.Date.valueOf(today.plusDays(2)));

		new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> execution.activate(request, today, now));

		assertThat(jdbc.queryForObject("select status from bookings where id=?", String.class, booking))
			.isEqualTo("CANCELLED");
		assertThat(jdbc.queryForObject("select status from session_waitlists where id=?", String.class, waitlist))
			.isEqualTo("PROMOTED");
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id=? and member_account_id=? and membership_id=? and status='BOOKED'",
			Long.class, session, waitingMember, waitingMembership)).isOne();
	}

	@Test
	void activationCancelsBookingAndPromotesWaitlistAfterCommit() {
		Instant now = Instant.now();
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
		UUID manager = account("MANAGER");
		UUID holder = account("MEMBER");
		UUID waiter = account("MEMBER");
		UUID coach = account("COACH");
		UUID holderMembership = membership(holder, manager, now);
		UUID waiterMembership = membership(waiter, manager, now);
		UUID session = futureSession(manager, coach, now.plusSeconds(3600), 1);
		UUID booking = UUID.randomUUID();
		jdbc.update("insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id) values(?,?,?,?,'BOOKED',?)",
			booking, session, holder, holderMembership, holder);
		UUID waitlist = UUID.randomUUID();
		jdbc.update("insert into session_waitlists(id,class_session_id,member_account_id,status,joined_by_account_id,source) values(?,?,?,'WAITING',?,'MEMBER')",
			waitlist, session, waiter, waiter);
		UUID request = UUID.randomUUID();
		jdbc.update("insert into membership_freeze_requests(id,membership_id,member_account_id,requester_account_id,approver_account_id,start_date,end_date,reason,fee_amount,status,approved_at) values(?,?,?,?,?,?,?,'integration',0,'SCHEDULED',?)",
			request, holderMembership, holder, holder, manager, java.sql.Date.valueOf(today),
			java.sql.Date.valueOf(today.plusDays(2)), Timestamp.from(now.minusSeconds(60)));
		jdbc.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,status) values(?,?,?,?,?,'SCHEDULED')",
			UUID.randomUUID(), request, holderMembership, java.sql.Date.valueOf(today),
			java.sql.Date.valueOf(today.plusDays(2)));

		new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> execution.activate(request, today, now));

		assertThat(jdbc.queryForObject("select status from bookings where id=?", String.class, booking))
			.isEqualTo("CANCELLED");
		assertThat(jdbc.queryForObject("select cancellation_source from bookings where id=?", String.class, booking))
			.isEqualTo("MEMBERSHIP_FREEZE");
		assertThat(jdbc.queryForObject("select status from session_waitlists where id=?", String.class, waitlist))
			.isEqualTo("PROMOTED");
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id=? and member_account_id=? "
			+ "and membership_id=? and status='BOOKED'", Long.class, session, waiter, waiterMembership)).isOne();
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?, 'ACTIVE',?,?,?,?,?)",
			id, role, "Freeze " + role, "09" + id.toString().replace("-", "").substring(0, 9), id + "@freeze.test",
			java.sql.Date.valueOf("1990-01-01"), "hash");
		return id;
	}

	private UUID membership(UUID member, UUID manager, Instant now) {
		UUID offer = UUID.randomUUID(); UUID order = UUID.randomUUID(); UUID id = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'PLUS','Freeze offer','Freeze offer',100000,'VND',30,'ACTIVE',?)", offer, manager);
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,expires_at,paid_at,fulfillment_mode) values(?,?,?,? ,?,'Freeze offer','PLUS',100000,'VND',30,'CASH','PAID',?,?, 'IMMEDIATE')",
			order, "FREEZE-" + order.toString().replace("-", ""), member, manager, offer, Timestamp.from(now.plusSeconds(3600)), Timestamp.from(now));
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at) values(?,?,'CASH','PAID',100000,'VND',?,?)",
			UUID.randomUUID(), order, manager, Timestamp.from(now));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Freeze offer',100000,'VND',30,'ACTIVE',?,?)",
			id, member, order, offer, "PLUS", Timestamp.from(now.minusSeconds(86400)), Timestamp.from(now.plusSeconds(86400)));
		return id;
	}

	private UUID futureSession(UUID manager, UUID coach, Instant start) {
		return futureSession(manager, coach, start, 10);
	}

	private UUID futureSession(UUID manager, UUID coach, Instant start, int capacity) {
		UUID discipline = UUID.randomUUID(); UUID sportClass = UUID.randomUUID(); UUID room = UUID.randomUUID(); UUID session = UUID.randomUUID();
		jdbc.update("insert into disciplines(id,name,description,status) values(?,?,?,'ACTIVE')", discipline, "Freeze discipline " + discipline, "Freeze");
		jdbc.update("insert into sport_classes(id,discipline_id,name,class_type,description,status) values(?,?,?,'GROUP',?,'ACTIVE')", sportClass, discipline, "Freeze class " + sportClass, "Freeze");
		jdbc.update("insert into rooms(id,name,capacity,status) values(?,?,10,'ACTIVE')", room, "Freeze room " + room);
		jdbc.update("insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,capacity,status,created_by_account_id) values(?,?,?,?,?,?,?,'SCHEDULED',?)",
			session, sportClass, coach, room, Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)), capacity,
			manager);
		return session;
	}
}
