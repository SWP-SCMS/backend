package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.course.CourseRunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseRunReportingIntegrationTests {
	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired JdbcTemplate db;
	@Autowired ManagerReportingService reports;
	private UUID managerId;
	private UUID coachId;
	private UUID courseId;

	@BeforeEach
	void setUp() {
		managerId = account("MANAGER");
		coachId = account("COACH");
		UUID discipline = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", discipline,
			"Reporting discipline " + discipline);
		courseId = UUID.randomUUID();
		db.update("insert into courses(id,code,name,discipline_id,status) values(?,?,?,?,'PUBLISHED')",
			courseId, "REPORT-" + shortId(courseId), "Reporting course", discipline);
	}

	@Test
	void aggregatesCourseLifecycleAttendanceMoneyRefundAndWaitlistWithoutNulls() {
		UUID runId = run(Instant.parse("2026-11-01T02:00:00Z"), "COMPLETED");
		Commerce paid = commerce(runId, "PAID", "ACTIVE");
		commerce(runId, "PENDING", null);
		commerce(runId, "FAILED", null);
		attendance(runId, paid.enrollmentId(), "PRESENT");
		attendance(runId, paid.enrollmentId(), "NOT_RECORDED");
		db.update("insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd) "
			+ "values(?,?,?,'UNRESOLVED_SESSIONS',500)", UUID.randomUUID(), paid.enrollmentId(), runId);
		UUID waitingMember = account("MEMBER");
		db.update("insert into course_waitlists(id,course_run_id,member_account_id,status) values(?,?,?,'WAITING')",
			UUID.randomUUID(), runId, waitingMember);

		CourseRunReportItem item = reports.courseRuns(managerId, runId, CourseRunStatus.COMPLETED,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), 0, 20).content().getFirst();

		assertThat(item.activeEnrollments()).isOne();
		assertThat(item.attendancePresent()).isOne();
		assertThat(item.attendanceNotRecorded()).isOne();
		assertThat(item.grossAmountVnd()).isEqualTo(BigInteger.valueOf(3_000));
		assertThat(item.paidAmountVnd()).isEqualTo(BigInteger.valueOf(1_000));
		assertThat(item.pendingPayments()).isOne();
		assertThat(item.pendingPaymentAmountVnd()).isEqualTo(BigInteger.valueOf(1_000));
		assertThat(item.failedPayments()).isOne();
		assertThat(item.failedPaymentAmountVnd()).isEqualTo(BigInteger.valueOf(1_000));
		assertThat(item.outstandingRefundObligations()).isOne();
		assertThat(item.outstandingRefundAmountVnd()).isEqualTo(BigInteger.valueOf(500));
		assertThat(item.waitlistWaiting()).isOne();
		assertThat(item.completedRefundAmountVnd()).isZero();
		assertThat(item.transferCompleted()).isZero();
	}

	@Test
	void usesVietnamHalfOpenDatesAndStablePageBoundaries() {
		UUID before = run(Instant.parse("2026-10-31T16:59:59Z"), "COMPLETED");
		UUID first = run(Instant.parse("2026-10-31T17:00:00Z"), "COMPLETED");
		UUID second = run(Instant.parse("2026-11-01T16:59:59Z"), "COMPLETED");
		run(Instant.parse("2026-11-01T17:00:00Z"), "COMPLETED");

		CourseRunReportPageResponse page0 = reports.courseRuns(managerId, null, CourseRunStatus.COMPLETED,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), 0, 1);
		CourseRunReportPageResponse page1 = reports.courseRuns(managerId, null, CourseRunStatus.COMPLETED,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), 1, 1);

		assertThat(page0.totalElements()).isEqualTo(2);
		assertThat(page0.totalPages()).isEqualTo(2);
		assertThat(page0.content()).extracting(CourseRunReportItem::runId).containsExactly(second);
		assertThat(page1.content()).extracting(CourseRunReportItem::runId).containsExactly(first);
		assertThat(page0.content()).extracting(CourseRunReportItem::runId).doesNotContain(before);
		assertThat(page1.content().getFirst().grossAmountVnd()).isZero();
	}

	private UUID run(Instant start, String status) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
			 minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,planned_end_at,
			 total_units,named_coach_commitment,policy_version_snapshot,status)
			values(?,?,?,?,1000,20,1,?,?,?,?,2,true,'policy-v1',?)
			""", id, "RUN-" + shortId(id), courseId, coachId, Timestamp.from(start.minusSeconds(7200)),
			Timestamp.from(start.minusSeconds(3600)), Timestamp.from(start), Timestamp.from(start.plusSeconds(7200)),
			status);
		return id;
	}

	private Commerce commerce(UUID runId, String paymentStatus, String lifecycle) {
		UUID member = account("MEMBER");
		UUID order = UUID.randomUUID();
		UUID reservation = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		Instant created = Instant.parse("2026-10-01T00:00:00Z");
		Instant deadline = created.plusSeconds(3600);
		String reference = "COR-" + order.toString().replace("-", "").toUpperCase();
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key,created_at) values(?,?,?,?,?,1000,'VND',?,?,?)",
			order, reference, member, managerId, runId, "PAID".equals(paymentStatus) ? "PAID" : "PENDING_PAYMENT",
			"report-" + order, Timestamp.from(created));
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,created_at) "
			+ "values(?,?,?,?,?,?,?)", reservation, order, member, runId,
			"PAID".equals(paymentStatus) ? "ENROLLED" : "HELD", Timestamp.from(deadline), Timestamp.from(created));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,"
			+ "payment_reference,amount,currency_code,status,deadline_at,method,processed_by_account_id,paid_at,created_at) "
			+ "values(?,?,?,?,?,?,1000,'VND',? ,?,'CASH',?,?,?)", payment, order, reservation, member, managerId,
			reference, paymentStatus, Timestamp.from(deadline), "PAID".equals(paymentStatus) ? managerId : null,
			"PAID".equals(paymentStatus) ? Timestamp.from(created) : null, Timestamp.from(created));
		if (lifecycle == null) return new Commerce(null);
		UUID enrollment = UUID.randomUUID();
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,"
			+ "member_account_id,actor_account_id,processed_by_account_id,course_run_id,amount_paid_vnd_snapshot,"
			+ "total_units_snapshot,enrolled_at,lifecycle_status,cash_paid_vnd) values(?,?,?,?,?,?,?,?,1000,2,?,?,1000)",
			enrollment, order, reservation, payment, member, managerId, managerId, runId, Timestamp.from(created), lifecycle);
		return new Commerce(enrollment);
	}

	private void attendance(UUID runId, UUID enrollmentId, String status) {
		UUID room = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,status) values(?,?,20,'ACTIVE')", room, "Room " + room);
		UUID session = UUID.randomUUID();
		Instant start = Instant.parse("2026-11-01T03:00:00Z");
		db.update("insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at,"
			+ "original_start_at,original_end_at,allocation_vnd,status) values(?,?,?,?,?,?,?,?,?,500,'COMPLETED')",
			session, runId, "PRESENT".equals(status) ? 1 : 2, room, coachId, Timestamp.from(start),
			Timestamp.from(start.plusSeconds(3600)), Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)));
		boolean recorded = !"NOT_RECORDED".equals(status);
		db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,"
			+ "unit_number,allocation_vnd,disposition,attendance_status,attendance_recorded_at,"
			+ "attendance_recorded_by,attendance_updated_at) values(?,?,?,?,?,500,'ENROLLED',?,?,?,?)",
			UUID.randomUUID(), enrollmentId, session, runId, "PRESENT".equals(status) ? 1 : 2, status,
			recorded ? Timestamp.from(start.plusSeconds(3600)) : null, recorded ? coachId : null,
			recorded ? Timestamp.from(start.plusSeconds(3600)) : null);
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) "
			+ "values(?,?,'ACTIVE',?,?,?,date '1990-01-01','{noop}password')", id, role, role + " report",
			"09" + shortId(id), id + "@report.test");
		return id;
	}

	private String shortId(UUID id) { return id.toString().replace("-", "").substring(0, 8); }

	private record Commerce(UUID enrollmentId) { }
}
