package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

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
class MembershipTerminationExecutionIntegrationTests {
	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired JdbcTemplate db;
	@Autowired MembershipTerminationExecutionService execution;

	@Test
	void immediateExecutionCleansUpAllFutureMemberActivityAtomically() {
		Instant now = Instant.now();
		UUID manager = account("MANAGER");
		UUID member = account("MEMBER");
		UUID coach = account("COACH");
		UUID offer = offer(manager);
		UUID membership = membership(member, manager, offer, now);
		UUID visit = openVisit(member, membership, now.minusSeconds(600));
		UUID session = futureSession(manager, coach, now.plusSeconds(3600));
		UUID booking = booking(member, membership, session, now);
		UUID assignment = assignment(member, coach, manager, now.minusSeconds(1800));
		UUID request = approvedRequest(membership, member, manager, now);

		execution.executeImmediate(request, manager, now);

		assertThat(status("memberships", membership)).isEqualTo("TERMINATED");
		assertThat(db.queryForObject("select checkout_source from center_visits where id=?", String.class, visit))
			.isEqualTo("MEMBERSHIP_TERMINATION");
		assertThat(status("bookings", booking)).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select cancellation_source from bookings where id=?", String.class, booking))
			.isEqualTo("MEMBERSHIP_TERMINATED");
		assertThat(status("coach_assignments", assignment)).isEqualTo("ENDED");
		assertThat(status("membership_termination_requests", request)).isEqualTo("EXECUTED");
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action=?", Long.class,
			request, "MEMBERSHIP_TERMINATION_EXECUTED")).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where target_id=? and notification_type=?",
			Long.class, request, "MEMBERSHIP_TERMINATION_EXECUTED")).isOne();
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?, 'ACTIVE',?,?,?,?,?)",
			id, role, "Termination " + role, "09" + id.toString().replace("-", "").substring(0, 9),
			id + "@termination.test", java.sql.Date.valueOf("1990-01-01"), "hash");
		return id;
	}

	private UUID offer(UUID manager) {
		UUID id = UUID.randomUUID();
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?, 'PLUS','Termination offer','Termination offer',100000,'VND',30,'ACTIVE',?)",
			id, manager);
		return id;
	}

	private UUID membership(UUID member, UUID manager, UUID offer, Instant now) {
		UUID order = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,expires_at,paid_at,fulfillment_mode) values(?,?,?,? ,?,'Termination offer','PLUS',100000,'VND',30,'CASH','PAID',?,?, 'IMMEDIATE')",
			order, "TERM-" + order, member, manager, offer, java.sql.Timestamp.from(now.plusSeconds(3600)),
			java.sql.Timestamp.from(now));
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Termination offer',100000,'VND',30,'ACTIVE',?,?)",
			id, member, order, offer, "PLUS", java.sql.Timestamp.from(now.minusSeconds(86400)),
			java.sql.Timestamp.from(now.plusSeconds(86400)));
		return id;
	}

	private UUID openVisit(UUID member, UUID membership, Instant checkedInAt) {
		UUID visit = UUID.randomUUID();
		db.update("insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at) values(?,?,?,?,?)",
			visit, member, membership, member, java.sql.Timestamp.from(checkedInAt));
		return visit;
	}

	private UUID futureSession(UUID manager, UUID coach, Instant start) {
		UUID discipline = UUID.randomUUID();
		UUID sportClass = UUID.randomUUID();
		UUID room = UUID.randomUUID();
		UUID session = UUID.randomUUID();
		db.update("insert into disciplines(id,name,description,status) values(?,?,?,'ACTIVE')", discipline,
			"Termination discipline " + discipline, "Termination");
		db.update("insert into sport_classes(id,discipline_id,name,class_type,description,status) values(?,?,?,'GROUP',?,'ACTIVE')",
			sportClass, discipline, "Termination class " + sportClass, "Termination");
		db.update("insert into rooms(id,name,capacity,status) values(?,?,10,'ACTIVE')", room,
			"Termination room " + room);
		db.update("insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,capacity,status,created_by_account_id) values(?,?,?,?,?,? ,10,'SCHEDULED',?)",
			session, sportClass, coach, room, java.sql.Timestamp.from(start),
			java.sql.Timestamp.from(start.plusSeconds(3600)), manager);
		return session;
	}

	private UUID booking(UUID member, UUID membership, UUID session, Instant now) {
		UUID id = UUID.randomUUID();
		db.update("insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id) values(?,?,?,?,'BOOKED',?)",
			id, session, member, membership, member);
		return id;
	}

	private UUID assignment(UUID member, UUID coach, UUID manager, Instant startsAt) {
		UUID id = UUID.randomUUID();
		db.update("insert into coach_assignments(id,member_account_id,coach_account_id,assigned_by_account_id,status,starts_at) values(?,?,?,?, 'ACTIVE',?)",
			id, member, coach, manager, java.sql.Timestamp.from(startsAt));
		return id;
	}

	private UUID approvedRequest(UUID membership, UUID member, UUID manager, Instant now) {
		UUID id = UUID.randomUUID();
		db.update("insert into membership_termination_requests(id,membership_id,member_account_id,requester_account_id,approver_account_id,mode,status,reason,effective_at,approved_at) values(?,?,?,?,?,'IMMEDIATE','APPROVED','integration',?,?)",
			id, membership, member, member, manager, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
		return id;
	}

	private String status(String table, UUID id) {
		return db.queryForObject("select status from " + table + " where id=?", String.class, id);
	}
}
