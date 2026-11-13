package com.scms.backend.course.withdrawal;

import static com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.CourseWithdrawalException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseWithdrawalRepository {

	private final JdbcTemplate db;

	CourseWithdrawalRepository(JdbcTemplate db) {
		this.db = db;
	}

	String lockRun(UUID runId) {
		return db.queryForObject("select status from course_runs where id=? for update", String.class, runId);
	}

	Map<String, Object> lockEnrollment(UUID enrollmentId, UUID memberId) {
		return db.queryForList("select id,course_run_id,lifecycle_status,total_units_snapshot,amount_paid_vnd_snapshot from course_enrollments " + "where id=? and member_account_id=? for update", enrollmentId, memberId).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	WithdrawalRequest lock(UUID id) {
		return db.query(select() + " where w.id=? for update of w", this::map, id).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	WithdrawalRequest find(UUID id) {
		return db.query(select() + " where w.id=?", this::map, id).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	String select() {
		return """
			select w.*,a.id member_id,mp.member_code,a.full_name member_name,c.id course_id,
			c.code course_code,c.name course_name,r.run_code,r.status course_run_status,e.lifecycle_status,
			refund.id refund_obligation_id,refund.status refund_status
			from course_withdrawal_requests w join course_enrollments e on e.id=w.course_enrollment_id
			join accounts a on a.id=e.member_account_id join member_profiles mp on mp.account_id=a.id
			join course_runs r on r.id=e.course_run_id join courses c on c.id=r.course_id
			left join course_refund_obligations refund on w.status='APPROVED'
			and refund.course_enrollment_id=w.course_enrollment_id and refund.source_event_id is null
			and refund.source_type=case when w.cause_code='VOLUNTARY' then 'MEMBER_WITHDRAWAL' else 'MATERIAL_CHANGE' end
			""";
	}

	WithdrawalRequest map(ResultSet rs, int row) throws SQLException {
		Instant capturedAt = instant(rs.getTimestamp("snapshot_captured_at"));
		boolean pending = "PENDING".equals(rs.getString("status"));
		boolean eligible = pending && capturedAt != null && !"CANCELLED".equals(rs.getString("course_run_status"));
		BigInteger refund = amount(rs.getBigDecimal("refund_amount_vnd"));
		String source = capturedAt != null ? "REQUEST_TIME" : refund != null ? "LEGACY_APPROVAL" : "UNAVAILABLE";
		String eligibilityReason = eligible ? rs.getString("withdrawal_reason_snapshot") : capturedAt == null ? "LEGACY_SNAPSHOT_MISSING" : "REQUEST_NOT_PENDING";
		return new WithdrawalRequest(rs.getObject("id", UUID.class), rs.getObject("course_enrollment_id", UUID.class), rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getObject("requester_account_id", UUID.class), rs.getString("status"), rs.getString("request_reason"), rs.getString("decision_reason"), CourseWithdrawalCause.valueOf(rs.getString("cause_code")), eligible, eligibilityReason, rs.getTimestamp("requested_at").toInstant(), rs.getObject("decided_by_account_id", UUID.class), instant(rs.getTimestamp("decided_at")), rs.getObject("total_units_snapshot", Integer.class), rs.getObject("remaining_units_snapshot", Integer.class), amount(rs.getBigDecimal("refund_basis_vnd")), refund, rs.getObject("member_id", UUID.class), rs.getString("member_code"), rs.getString("member_name"), rs.getObject("course_id", UUID.class), rs.getString("course_code"), rs.getString("course_name"), rs.getString("run_code"), rs.getString("course_run_status"), eligible ? refund : null, rs.getObject("schedule_change_response_id", UUID.class), rs.getObject("change_event_id", UUID.class), capturedAt, source, rs.getObject("withdrawal_eligible_snapshot", Boolean.class), rs.getString("withdrawal_reason_snapshot"), instant(rs.getTimestamp("cancelled_at")), rs.getObject("cancelled_by_account_id", UUID.class), rs.getString("cancellation_reason"), pending, "WITHDRAWAL_PENDING".equals(rs.getString("lifecycle_status")), rs.getObject("refund_obligation_id", UUID.class), rs.getString("refund_status"));
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return new BigDecimal(row.get(key).toString()).toBigIntegerExact();
	}

	static BigInteger amount(BigDecimal value) {
		return value == null ? null : value.toBigIntegerExact();
	}

	static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value instanceof Instant instant ? instant : ((Timestamp) value).toInstant();
	}

	static Instant instant(Timestamp value) {
		return value == null ? null : value.toInstant();
	}

	List<Map<java.lang.String, java.lang.Object>> createForScheduleChangeFindCourseScheduleChangeResponses(UUID responseId, UUID memberId) {
		return db.queryForList("""
			select r.id response_id,r.response,r.status,r.cause_code,r.change_event_id,
				es.course_enrollment_id,e.course_run_id,e.member_account_id
			from course_schedule_change_responses r
			join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			where r.id=? and e.member_account_id=?
			""", responseId, memberId);
	}

	List<Map<java.lang.String, java.lang.Object>> createForScheduleChangeFindCourseWithdrawalRequests(UUID responseId) {
		return db.queryForList("select id from course_withdrawal_requests where schedule_change_response_id=?", responseId);
	}

	List<Map<java.lang.String, java.lang.Object>> findPendingWithdrawalForEnrollment(UUID enrollmentId) {
		return db.queryForList("select id from course_withdrawal_requests where course_enrollment_id=? and status='PENDING'", enrollmentId);
	}

	java.lang.Boolean createForScheduleChangeFindCourseTransferRequests(UUID enrollmentId) {
		return db.queryForObject("select exists(select 1 from course_transfer_requests where " + "source_enrollment_id=? and status in ('PENDING','OFFERED'))", Boolean.class, enrollmentId);
	}

	List<com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest> listForMemberFindRows(UUID memberId) {
		return db.query(select() + " where w.member_account_id=? order by w.requested_at desc,w.id desc", this::map, memberId);
	}

	List<com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest> detailForMemberFindRows(UUID requestId, UUID memberId) {
		return db.query(select() + " where w.id=? and w.member_account_id=?", this::map, requestId, memberId);
	}

	List<com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest> managerQueueFindRows() {
		return db.query(select() + " order by case when w.status='PENDING' then 0 else 1 end,w.requested_at,w.id", this::map);
	}

	int approveUpdateCourseEnrollments(UUID enrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='WITHDRAWN' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", enrollmentId);
	}

	int approveUpdateCourseReservations(Timestamp nowTimestamp, UUID enrollmentId) {
		return db.update("update course_reservations set status='WITHDRAWN',updated_at=? where id=(select course_reservation_id " + "from course_enrollments where id=?) and status='ENROLLED'", nowTimestamp, enrollmentId);
	}

	int approveUpdateCourseWithdrawalRequests(java.lang.String normalizedReason, UUID managerId, Timestamp nowTimestamp, UUID requestId) {
		return db.update("""
			update course_withdrawal_requests set status='APPROVED',decision_reason=?,decided_by_account_id=?,decided_at=?
			where id=? and status='PENDING'
			""", normalizedReason, managerId, nowTimestamp, requestId);
	}

	int approveInsertCourseRefundObligations(UUID randomUUID, UUID enrollmentId, UUID courseRunId, java.lang.Object value4, BigInteger refund, UUID requesterAccountId, java.lang.String requestReason) {
		return db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,
			 requester_account_id,request_reason)
			values(?,?,?, ?,?,?,?) on conflict(course_enrollment_id,source_type,source_event_id) do nothing
			""", randomUUID, enrollmentId, courseRunId, value4, refund, requesterAccountId, requestReason);
	}

	int rejectUpdateCourseEnrollments(UUID enrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", enrollmentId);
	}

	int rejectUpdateCourseWithdrawalRequests(java.lang.String normalizedReason, UUID managerId, Timestamp nowTimestamp, UUID requestId) {
		return db.update("update course_withdrawal_requests set status='REJECTED',decision_reason=?,decided_by_account_id=?,decided_at=? where id=? and status='PENDING'", normalizedReason, managerId, nowTimestamp, requestId);
	}

	int cancelUpdateCourseWithdrawalRequests(Timestamp value1, UUID actorId, java.lang.String normalizedReason, UUID requestId) {
		return db.update("update course_withdrawal_requests set status='CANCELLED',cancelled_at=?,cancelled_by_account_id=?," + "cancellation_reason=? where id=? and status='PENDING'", value1, actorId, normalizedReason, requestId);
	}

	int cancelUpdateCourseEnrollments(UUID enrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", enrollmentId);
	}

	List<Map<java.lang.String, java.lang.Object>> createFindCourseEnrollments(UUID enrollmentId, UUID memberId) {
		return db.queryForList("select course_run_id from course_enrollments where id=? and member_account_id=?", enrollmentId, memberId);
	}

	java.lang.Boolean createFindCourseWithdrawalRequests(UUID enrollmentId) {
		return db.queryForObject("select exists(select 1 from course_withdrawal_requests where course_enrollment_id=? and status='PENDING')", Boolean.class, enrollmentId);
	}

	java.lang.Boolean createFindCourseTransferRequests(UUID enrollmentId) {
		return db.queryForObject("select exists(select 1 from course_transfer_requests where " + "source_enrollment_id=? and status in ('PENDING','OFFERED'))", Boolean.class, enrollmentId);
	}

	int insertRequestInsertCourseWithdrawalRequests(UUID requestId, UUID enrollmentId, UUID runId, UUID memberId, UUID requesterId, java.lang.String normalizedReason, Timestamp nowTimestamp, java.lang.String cause, UUID responseId, UUID eventId, Timestamp nowTimestamp2, int totalUnits, int remainingUnits, BigInteger refundBasisVnd, BigInteger refundAmountVnd, boolean eligible, java.lang.String reason) {
		return db.update("""
			insert into course_withdrawal_requests(id,course_enrollment_id,course_run_id,member_account_id,
			 requester_account_id,request_reason,requested_at,cause_code,schedule_change_response_id,change_event_id,
			 snapshot_captured_at,total_units_snapshot,remaining_units_snapshot,refund_basis_vnd,refund_amount_vnd,
			 withdrawal_eligible_snapshot,withdrawal_reason_snapshot)
			values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			""", requestId, enrollmentId, runId, memberId, requesterId, normalizedReason, nowTimestamp, cause, responseId, eventId, nowTimestamp2, totalUnits, remainingUnits, refundBasisVnd, refundAmountVnd, eligible, reason);
	}

	int insertRequestUpdateCourseEnrollments(UUID enrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='WITHDRAWAL_PENDING' where id=? and lifecycle_status='ACTIVE'", enrollmentId);
	}

	List<UUID> insertRequestFindAccounts() {
		return db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE' order by id", UUID.class);
	}

	List<Map<java.lang.String, java.lang.Object>> refundSnapshotFindCourseEnrollmentSessions(UUID enrollmentId) {
		return db.queryForList("""
			select s.start_at,es.allocation_vnd from course_enrollment_sessions es
			join course_sessions s on s.id=es.course_session_id
			where es.course_enrollment_id=? and not exists (select 1 from course_refund_unit_claims c where c.enrollment_session_id=es.id) order by es.unit_number for update of es
			""", enrollmentId);
	}
}
