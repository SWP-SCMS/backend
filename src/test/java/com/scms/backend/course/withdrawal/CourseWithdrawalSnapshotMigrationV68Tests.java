package com.scms.backend.course.withdrawal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CourseWithdrawalSnapshotMigrationV68Tests {

	private static final Instant NOW = Instant.parse("2035-01-15T00:00:00Z");

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	private JdbcTemplate db;

	private UUID memberId, coachId, runId, enrollmentId;

	@BeforeEach
	void prepareDatabase() {
		flyway(null).clean();
		db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	@Test
	void upgradeClosesOnlyLegacyPendingAndPreservesPaymentsAttendanceAndPaidRefunds() {
		flyway("67").migrate();
		fixture();
		UUID legacyEnrollment = enrollmentId, legacyMember = memberId;
		UUID rejected = legacyRequest("REJECTED");
		UUID pending = legacyRequest("PENDING");
		db.update("update course_enrollments set lifecycle_status='WITHDRAWAL_PENDING' where id=?", enrollmentId);
		db.update("update course_enrollment_sessions set attendance_status='PRESENT',attendance_recorded_at=?,attendance_updated_at=?,attendance_recorded_by=? where course_enrollment_id=? and unit_number=1", Timestamp.from(NOW), Timestamp.from(NOW), coachId, enrollmentId);
		fixture();
		UUID approved = legacyRequest("APPROVED");
		db.update("update course_enrollments set lifecycle_status='WITHDRAWN' where id=?", enrollmentId);
		db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,status,
			 approved_amount_vnd,approved_by_account_id,approval_reason,approved_at,disbursed_amount_vnd,
			 disbursed_by_account_id,disbursed_at,disbursement_method,disbursement_reference,evidence_reference,disbursement_idempotency_key)
			values(?,?,?,'MEMBER_WITHDRAWAL',560000,'DISBURSEMENT_RECORDED',560000,?,'Approved',?,560000,?,?,'CASH','receipt','evidence','paid-key')
			""", UUID.randomUUID(), enrollmentId, runId, memberId, Timestamp.from(NOW), memberId, Timestamp.from(NOW));
		fixture();
		UUID untouchedEnrollment = enrollmentId;
		var payments = db.queryForList("select * from course_payments order by id");
		var attendance = db.queryForList("select * from course_enrollment_sessions order by id");
		var refunds = db.queryForList("select * from course_refund_obligations order by id");
		// V75 adds a nullable event key; every existing historical value must remain unchanged.
		refunds.forEach(row -> row.put("source_event_id", null));
		var approvedBefore = db.queryForMap("select status,requested_at,decided_at,refund_amount_vnd,refund_basis_vnd from course_withdrawal_requests where id=?", approved);
		flyway(null).migrate();
		flyway(null).migrate();
		flyway(null).validate();
		assertThat(db.queryForMap("select status,cancellation_reason from course_withdrawal_requests where id=?", pending)).containsEntry("status", "CANCELLED").containsEntry("cancellation_reason", "LEGACY_SNAPSHOT_MISSING");
		assertThat(db.queryForObject("select snapshot_captured_at from course_withdrawal_requests where id=?", Timestamp.class, pending)).isNull();
		assertThat(db.queryForObject("select refund_amount_vnd from course_withdrawal_requests where id=?", java.math.BigDecimal.class, pending)).isNull();
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, legacyEnrollment)).isEqualTo("ACTIVE");
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, untouchedEnrollment)).isEqualTo("ACTIVE");
		assertThat(db.queryForObject("select status from course_withdrawal_requests where id=?", String.class, rejected)).isEqualTo("REJECTED");
		assertThat(db.queryForMap("select status,requested_at,decided_at,refund_amount_vnd,refund_basis_vnd from course_withdrawal_requests where id=?", approved)).isEqualTo(approvedBefore);
		assertThat(db.queryForList("select * from course_payments order by id")).isEqualTo(payments);
		assertThat(db.queryForList("select * from course_enrollment_sessions order by id")).isEqualTo(attendance);
		assertThat(db.queryForList("select * from course_refund_obligations order by id")).isEqualTo(refunds);
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_WITHDRAWAL_CANCELLED'", Long.class, pending)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where target_id=? and recipient_account_id=?", Long.class, pending, legacyMember)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where target_id in (?,?)", Long.class, approved, rejected)).isZero();
	}

	@Test
	void freshDatabaseRequiresCompleteSnapshotAndRejectsSnapshotMutation() {
		flyway(null).migrate();
		fixture();
		assertThatThrownBy(() -> legacyRequest("PENDING")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		UUID request = UUID.randomUUID();
		db.update("""
			insert into course_withdrawal_requests(id,course_enrollment_id,course_run_id,member_account_id,requester_account_id,
			 request_reason,requested_at,snapshot_captured_at,total_units_snapshot,remaining_units_snapshot,
			 refund_basis_vnd,refund_amount_vnd,withdrawal_eligible_snapshot,withdrawal_reason_snapshot)
			values(?,?,?,?,?,'New snapshot',?,?,10,7,700000,560000,true,'ELIGIBLE_WITH_REFUND')
			""", request, enrollmentId, runId, memberId, memberId, Timestamp.from(NOW), Timestamp.from(NOW));
		assertThatThrownBy(() -> db.update("update course_withdrawal_requests set remaining_units_snapshot=6 where id=?", request)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("update course_withdrawal_requests set cause_code='REDUCED_UNIT_COUNT' where id=?", request)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	private UUID legacyRequest(String state) {
		UUID id = UUID.randomUUID();
		boolean decided = !state.equals("PENDING"), approved = state.equals("APPROVED");
		db.update("""
			insert into course_withdrawal_requests(id,course_enrollment_id,course_run_id,member_account_id,requester_account_id,
			 status,request_reason,requested_at,decision_reason,decided_by_account_id,decided_at,
			 total_units_snapshot,remaining_units_snapshot,refund_basis_vnd,refund_amount_vnd)
			values(?,?,?,?,?,?,'Legacy',?,?,?,?,?,?,?,?)
			""", id, enrollmentId, runId, memberId, memberId, state, Timestamp.from(NOW), decided ? "Historical decision" : null, decided ? memberId : null, decided ? Timestamp.from(NOW) : null, approved ? 10 : null, approved ? 7 : null, approved ? 700000 : null, approved ? 560000 : null);
		return id;
	}

	private void fixture() {
		memberId = account("MEMBER");
		coachId = account("COACH");
		db.update("insert into member_profiles(account_id) values(?)", memberId);
		UUID discipline = UUID.randomUUID(), course = UUID.randomUUID();
		runId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", discipline, "D-" + discipline);
		db.update("insert into courses(id,code,name,discipline_id,status) values(?,?,?,?,'PUBLISHED')", course, "C-" + course, "Course", discipline);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,minimum_enrollment,
			 registration_opens_at,registration_closes_at,planned_start_at,planned_end_at,total_units,named_coach_commitment,policy_version_snapshot,status)
			values(?,?,?,?,1000000,10,1,?,?,?,?,10,true,'P1','OPEN_FOR_REGISTRATION')
			""", runId, "R-" + runId, course, coachId, Timestamp.from(NOW.minusSeconds(86400)), Timestamp.from(NOW.plusSeconds(86400)), Timestamp.from(NOW.plusSeconds(172800)), Timestamp.from(NOW.plusSeconds(86400 * 30)));
		seedEnrollment();
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE',?,?,?,date '1990-01-01','unused')", id, role, "Migration", "09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test");
		return id;
	}

	private void seedEnrollment() {
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values(?,?,10,'EXCLUSIVE','ACTIVE')", roomId, "Withdrawal room " + roomId);
		UUID[] sessions = new UUID[10];
		for (int unit = 1; unit <= 10; unit++) {
			Instant start = unit <= 2 ? NOW.minusSeconds((3L - unit) * 86400) : unit == 3 ? NOW : NOW.plusSeconds((long) (unit - 3) * 86400);
			sessions[unit - 1] = UUID.randomUUID();
			db.update("insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at," + "original_start_at,original_end_at,allocation_vnd,status) values(?,?,?,?,?,?,?,?,?,100000,'SCHEDULED')", sessions[unit - 1], runId, unit, roomId, coachId, Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)), Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)));
		}
		UUID orderId = UUID.randomUUID(), reservationId = UUID.randomUUID(), paymentId = UUID.randomUUID();
		enrollmentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id," + "tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,1000000,'VND','PAID',?)", orderId, reference, memberId, memberId, runId, "withdrawal-seed-" + orderId);
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) " + "values(?,?,?,?,'ENROLLED',?)", reservationId, orderId, memberId, runId, Timestamp.from(NOW.plusSeconds(86400)));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id," + "payment_reference,amount,currency_code,status,deadline_at,method,paid_at) " + "values(?,?,?,?,?,?,1000000,'VND','PAID',?,'CASH',?)", paymentId, orderId, reservationId, memberId, memberId, reference, Timestamp.from(NOW.plusSeconds(86400)), Timestamp.from(NOW));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id," + "member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at," + "cash_paid_vnd) values(?,?,?,?,?,?,?,1000000,10,?,1000000)", enrollmentId, orderId, reservationId, paymentId, memberId, memberId, runId, Timestamp.from(NOW));
		for (int unit = 1; unit <= 10; unit++) db.update("insert into course_enrollment_sessions(id,course_enrollment_id," + "course_session_id,course_run_id,unit_number,allocation_vnd,disposition) values(?,?,?,?,?,100000,'ENROLLED')", UUID.randomUUID(), enrollmentId, sessions[unit - 1], runId, unit);
	}

	private Flyway flyway(String target) {
		var config = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).cleanDisabled(false);
		if (target != null)
			config.target(target);
		return config.load();
	}
}
