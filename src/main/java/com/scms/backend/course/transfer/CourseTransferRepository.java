package com.scms.backend.course.transfer;

import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer;
import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferRequest;
import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferTarget;
import com.scms.backend.course.CourseTransferException;
import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
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

final class CourseTransferRepository {

	private final JdbcTemplate db;

	CourseTransferRepository(JdbcTemplate db) {
		this.db = db;
	}

	BigInteger unusedCredit(UUID enrollmentId, Instant now) {
		List<CourseTransferPolicy.UnitAllocation> units = db.query("""
			select s.start_at,es.allocation_vnd from course_enrollment_sessions es
			join course_sessions s on s.id=es.course_session_id where es.course_enrollment_id=? and not exists (select 1 from course_refund_unit_claims c where c.enrollment_session_id=es.id)
			""", (rs, n) -> new CourseTransferPolicy.UnitAllocation(rs.getTimestamp(1).toInstant(), rs.getBigDecimal(2).toBigIntegerExact()), enrollmentId);
		return CourseTransferPolicy.unusedCredit(units, now);
	}

	long usedSeats(UUID runId, Instant now) {
		return usedSeatsExcludingOffer(runId, null, now);
	}

	long usedSeatsExcludingOffer(UUID runId, UUID excludedOfferId, Instant now) {
		return db.queryForObject("""
			select (select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))
			 +(select count(*) from course_reservations where course_run_id=? and status='HELD' and expires_at>?)
			 +(select count(*) from course_waitlists where course_run_id=? and status='INVITED' and invitation_expires_at>?)
			 +(select count(*) from course_transfer_holds where target_run_id=? and status='HELD' and expires_at>?
			   and (?::uuid is null or transfer_offer_id<>?))
			""", Long.class, runId, runId, Timestamp.from(now), runId, Timestamp.from(now), runId, Timestamp.from(now), excludedOfferId, excludedOfferId);
	}

	boolean hasScheduleConflict(UUID memberId, UUID sourceRunId, UUID targetRunId) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists(select 1 from course_sessions target
			 join course_enrollment_sessions existing_session on existing_session.disposition='ENROLLED'
			 join course_enrollments existing on existing.id=existing_session.course_enrollment_id
			 join course_sessions existing_time on existing_time.id=existing_session.course_session_id
			 where target.course_run_id=? and target.status in ('SCHEDULED','MAKEUP_SCHEDULED')
			 and existing.member_account_id=? and existing.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') and existing.course_run_id<>?
			 and existing_time.start_at<target.end_at and existing_time.end_at>target.start_at)
			""", Boolean.class, targetRunId, memberId, sourceRunId));
	}

	TransferTarget target(ResultSet rs, Instant now) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		Timestamp next = rs.getTimestamp("next_session_at");
		return new TransferTarget(id, rs.getString("run_code"), rs.getString("course_code"), rs.getString("course_name"), rs.getBigDecimal("tuition_vnd").toBigIntegerExact(), rs.getInt("capacity"), usedSeats(id, now), rs.getTimestamp("registration_closes_at").toInstant(), next == null ? null : next.toInstant());
	}

	void lockRuns(UUID first, UUID second) {
		for (UUID id : List.of(first, second).stream().distinct().sorted().toList()) db.queryForObject("select id from course_runs where id=? for update", UUID.class, id);
	}

	int capacity(UUID runId) {
		return db.queryForObject("select capacity from course_runs where id=?", Integer.class, runId);
	}

	boolean activeEnrollmentExists(UUID memberId, UUID runId) {
		return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from course_enrollments where member_account_id=? and course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))", Boolean.class, memberId, runId));
	}

	TransferRequest lockRequest(UUID id) {
		return db.query(requestSelect() + " where tr.id=? for update of tr", this::mapRequest, id).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferRequest findRequest(UUID id) {
		return db.query(requestSelect() + " where tr.id=?", this::mapRequest, id).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferRequest findRequestByResponse(UUID id) {
		return db.query(requestSelect() + " where tr.schedule_change_response_id=?", this::mapRequest, id).stream().findFirst().orElse(null);
	}

	TransferOffer lockOffer(UUID memberId, UUID id) {
		return db.query(offerSelect() + " where transfer.id=? and transfer.member_account_id=? for update of transfer", this::mapOffer, id, memberId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferOffer findOffer(UUID id) {
		return db.query(offerSelect() + " where transfer.id=?", this::mapOffer, id).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferOffer findOfferByRequest(UUID id) {
		return db.query(offerSelect() + " where transfer.transfer_request_id=?", this::mapOffer, id).stream().findFirst().orElse(null);
	}

	CourseEnrollmentResponse targetEnrollment(UUID offerId) {
		Map<String, Object> row = rows("select id,course_order_id,member_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at from course_enrollments where source_transfer_offer_id=?", offerId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		return new CourseEnrollmentResponse(uuid(row, "id"), uuid(row, "course_order_id"), uuid(row, "member_account_id"), uuid(row, "course_run_id"), amount(row, "amount_paid_vnd_snapshot"), number(row, "total_units_snapshot"), instant(row, "enrolled_at"));
	}

	String requestSelect() {
		return "select tr.*,member.id member_id,mp.member_code member_code_display,member.full_name member_name_display," + "source_course.id source_course_id_display,source_course.code source_course_code_display,source_course.name source_course_name_display," + "source_run.run_code source_run_code_display,source_run.status source_course_run_status_display," + "target_course.id target_course_id_display,target_course.code target_course_code_display,target_course.name target_course_name_display," + "target_run.run_code target_run_code_display,target_run.status target_course_run_status_display " + "from course_transfer_requests tr join accounts member on member.id=tr.member_account_id " + "left join member_profiles mp on mp.account_id=member.id " + "left join course_runs source_run on source_run.id=tr.source_run_id " + "left join courses source_course on source_course.id=source_run.course_id " + "left join course_runs target_run on target_run.id=tr.target_run_id " + "left join courses target_course on target_course.id=target_run.course_id";
	}

	String offerSelect() {
		return "select transfer.*,member.id member_id,mp.member_code member_code_display,member.full_name member_name_display," + "source_course.id source_course_id_display,source_course.code source_course_code_display,source_course.name source_course_name_display," + "source_run.run_code source_run_code_display,source_run.status source_course_run_status_display," + "target_course.id target_course_id_display,target_course.code target_course_code_display,target_course.name target_course_name_display," + "target_run.run_code target_run_code_display,target_run.status target_course_run_status_display " + "from course_transfer_offers transfer join accounts member on member.id=transfer.member_account_id " + "left join member_profiles mp on mp.account_id=member.id " + "left join course_runs source_run on source_run.id=transfer.source_run_id " + "left join courses source_course on source_course.id=source_run.course_id " + "left join course_runs target_run on target_run.id=transfer.target_run_id " + "left join courses target_course on target_course.id=target_run.course_id";
	}

	TransferRequest mapRequest(ResultSet rs, int n) throws SQLException {
		return new TransferRequest(rs.getObject("id", UUID.class), rs.getObject("schedule_change_response_id", UUID.class), rs.getObject("source_enrollment_id", UUID.class), rs.getObject("source_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getString("status"), rs.getObject("target_run_id", UUID.class), rs.getString("reason"), rs.getTimestamp("requested_at").toInstant(), rs.getObject("decided_by_account_id", UUID.class), rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(), rs.getObject("member_id", UUID.class), rs.getString("member_code_display"), rs.getString("member_name_display"), rs.getObject("source_course_id_display", UUID.class), rs.getString("source_course_code_display"), rs.getString("source_course_name_display"), rs.getString("source_run_code_display"), rs.getString("source_course_run_status_display"), rs.getObject("target_course_id_display", UUID.class), rs.getString("target_course_code_display"), rs.getString("target_course_name_display"), rs.getString("target_run_code_display"), rs.getString("target_course_run_status_display"));
	}

	TransferOffer mapOffer(ResultSet rs, int n) throws SQLException {
		return new TransferOffer(rs.getObject("id", UUID.class), rs.getObject("transfer_request_id", UUID.class), rs.getObject("source_enrollment_id", UUID.class), rs.getObject("source_run_id", UUID.class), rs.getObject("target_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getBigDecimal("credit_vnd_snapshot").toBigIntegerExact(), rs.getBigDecimal("target_tuition_vnd_snapshot").toBigIntegerExact(), rs.getBigDecimal("difference_vnd_snapshot").toBigIntegerExact(), rs.getTimestamp("expires_at").toInstant(), rs.getString("status"), rs.getObject("difference_order_id", UUID.class), rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toInstant(), rs.getObject("member_id", UUID.class), rs.getString("member_code_display"), rs.getString("member_name_display"), rs.getObject("source_course_id_display", UUID.class), rs.getString("source_course_code_display"), rs.getString("source_course_name_display"), rs.getString("source_run_code_display"), rs.getString("source_course_run_status_display"), rs.getObject("target_course_id_display", UUID.class), rs.getString("target_course_code_display"), rs.getString("target_course_name_display"), rs.getString("target_run_code_display"), rs.getString("target_course_run_status_display"));
	}

	List<Map<String, Object>> rows(String sql, Object... args) {
		return db.queryForList(sql, args);
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return (UUID) row.get(key);
	}

	static int number(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).intValue();
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return new BigDecimal(row.get(key).toString()).toBigIntegerExact();
	}

	static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? null : value instanceof Instant i ? i : ((Timestamp) value).toInstant();
	}
}
