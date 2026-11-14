package com.scms.backend.course.refund;

import static com.scms.backend.course.refund.CourseRefundApiModels.ReceptionCourseRefund;
import static com.scms.backend.course.refund.CourseRefundApiModels.ReceptionCourseRefundPage;
import static com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation;
import com.scms.backend.course.CourseRefundException;
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

final class CourseRefundObligationRepository {

	private final JdbcTemplate db;

	CourseRefundObligationRepository(JdbcTemplate db) {
		this.db = db;
	}

	RefundObligation find(UUID id) {
		return db.query(select() + " where obligation.id=?", this::map, id).stream().findFirst().orElseThrow(CourseRefundException::notFound);
	}

	String select() {
		return "select obligation.*,profile.member_code display_member_code,member.full_name display_member_name," + "course.code display_course_code,course.name display_course_name,run.run_code display_run_code," + "run.status display_course_run_status from course_refund_obligations obligation " + "join accounts member on member.id=obligation.member_account_id " + "left join member_profiles profile on profile.account_id=member.id " + "join course_runs run on run.id=obligation.course_run_id " + "join courses course on course.id=run.course_id";
	}

	String receptionSelect() {
		return """
			select queue.*,queue.disbursement_eligibility_reason='ELIGIBLE' disbursement_eligible from (
			 select refund.id refund_id,refund.member_account_id member_id,profile.member_code,member.full_name member_name,member.phone member_phone,
			  course.id course_id,course.code course_code,course.name course_name,refund.course_run_id,run.run_code,
			  run.status course_run_status,refund.source_type source,refund.amount_vnd amount,refund.approved_at,
			  refund.version,
			  refund.status,case
			   when run.id is null then 'COURSE_RUN_NOT_FOUND'
			   when refund.source_type in ('COURSE_RUN_CANCELLED','PAYMENT_EXCEPTION','TRANSFER_CREDIT_EXCESS')
			    or run.status='COMPLETED' or (refund.source_type='REDUCED_SESSION_COUNT' and run.status='CANCELLED') then 'ELIGIBLE'
			   else 'AWAITING_COURSE_COMPLETION'
			  end disbursement_eligibility_reason
			 from course_refund_obligations refund
			 left join accounts member on member.id=refund.member_account_id
			 left join member_profiles profile on profile.account_id=refund.member_account_id
			 left join course_runs run on run.id=refund.course_run_id
			 left join courses course on course.id=run.course_id
			) queue
			""";
	}

	RefundObligation map(ResultSet rs, int row) throws SQLException {
		return new RefundObligation(rs.getObject("id", UUID.class), rs.getObject("course_enrollment_id", UUID.class), rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getString("display_member_code"), rs.getString("display_member_name"), rs.getString("display_course_code"), rs.getString("display_course_name"), rs.getString("display_run_code"), rs.getString("display_course_run_status"), rs.getObject("course_order_id", UUID.class), rs.getObject("course_payment_id", UUID.class), rs.getString("source_type"), rs.getString("status"), amount(rs.getBigDecimal("amount_vnd")), amount(rs.getBigDecimal("approved_amount_vnd")), amount(rs.getBigDecimal("disbursed_amount_vnd")), amount(rs.getBigDecimal("verified_amount_vnd")), rs.getString("currency_code").trim(), rs.getObject("requester_account_id", UUID.class), rs.getString("request_reason"), rs.getObject("approved_by_account_id", UUID.class), rs.getString("approval_reason"), instant(rs.getTimestamp("approved_at")), rs.getObject("rejected_by_account_id", UUID.class), rs.getString("rejection_reason"), instant(rs.getTimestamp("rejected_at")), rs.getObject("disbursed_by_account_id", UUID.class), instant(rs.getTimestamp("disbursed_at")), rs.getString("disbursement_method"), rs.getString("disbursement_reference"), rs.getString("evidence_reference"), rs.getObject("verified_by_account_id", UUID.class), rs.getString("verification_reason"), instant(rs.getTimestamp("verified_at")), rs.getObject("terminal_by_account_id", UUID.class), rs.getString("terminal_reason"), instant(rs.getTimestamp("terminal_at")), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
	}

	ReceptionCourseRefund mapReception(ResultSet rs, int row) throws SQLException {
		return new ReceptionCourseRefund(rs.getObject("refund_id", UUID.class), rs.getObject("member_id", UUID.class), rs.getString("member_code"), rs.getString("member_name"), rs.getObject("course_id", UUID.class), rs.getString("course_code"), rs.getString("course_name"), rs.getObject("course_run_id", UUID.class), rs.getString("run_code"), rs.getString("course_run_status"), rs.getString("source"), amount(rs.getBigDecimal("amount")), rs.getLong("version"), instant(rs.getTimestamp("approved_at")), rs.getBoolean("disbursement_eligible"), rs.getString("disbursement_eligibility_reason"));
	}

	static BigInteger amount(BigDecimal value) {
		return value == null ? null : value.toBigIntegerExact();
	}

	static Instant instant(Timestamp value) {
		return value == null ? null : value.toInstant();
	}

	ReceptionCourseRefundPage receptionRefundPage(int page, int size, Boolean eligible, String query) {
		String source = receptionSelect();
		String phone = CourseRefundValidation.phoneSearch(query);
		String filter = " where status='APPROVED' and (cast(? as boolean) is null or disbursement_eligible=?)"
			+ " and (cast(? as text) is null or strpos(lower(concat_ws(' ',member_name,member_code)),lower(cast(? as text)))>0"
			+ " or (cast(? as text) is not null and strpos(regexp_replace(coalesce(member_phone,''),'[^0-9]','','g'),cast(? as text))>0))";
		long total = db.queryForObject("select count(*) from (" + source + ") refunds" + filter, Long.class, eligible, eligible, query, query, phone, phone);
		List<ReceptionCourseRefund> items = db.query("select * from (" + source + ") refunds" + filter + " order by approved_at desc,refund_id desc limit ? offset ?", this::mapReception, eligible, eligible, query, query, phone, phone, size, (long) page * size);
		return new ReceptionCourseRefundPage(items, page, size, total, (int) ((total + size - 1) / size));
	}

	java.lang.Long listFindCourseRefundObligations(java.lang.String normalizedStatus, java.lang.String normalizedStatus2) {
		return db.queryForObject("select count(*) from course_refund_obligations where " + "(cast(? as varchar) is null or status=?)", Long.class, normalizedStatus, normalizedStatus2);
	}

	List<com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation> listFindRows(java.lang.String normalizedStatus, java.lang.String normalizedStatus2, int size, long value5) {
		return db.query(select() + " where (cast(? as varchar) is null or obligation.status=?) " + "order by case when obligation.status='PENDING_REVIEW' then 0 else 1 end,obligation.created_at,obligation.id limit ? offset ?", this::map, normalizedStatus, normalizedStatus2, size, value5);
	}

	java.lang.Long listForMemberFindCourseRefundObligations(UUID memberId) {
		return db.queryForObject("select count(*) from course_refund_obligations where member_account_id=?", Long.class, memberId);
	}

	List<com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation> listForMemberFindRows(UUID memberId, int size, long value4) {
		return db.query(select() + " where obligation.member_account_id=? " + "order by obligation.created_at desc,obligation.id desc limit ? offset ?", this::map, memberId, size, value4);
	}

	List<com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation> detailForMemberFindRows(UUID obligationId, UUID memberId) {
		return db.query(select() + " where obligation.id=? and obligation.member_account_id=?", this::map, obligationId, memberId);
	}

	List<com.scms.backend.course.refund.CourseRefundApiModels.ReceptionCourseRefund> detailForReceptionistFindRows(UUID refundId) {
		return db.query("select * from (" + receptionSelect() + ") refunds where refund_id=? and status='APPROVED'", this::mapReception, refundId);
	}

	int approveUpdateCourseRefundObligations(UUID managerId, java.lang.String normalizedReason, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID obligationId, java.lang.Long expectedVersion) {
		return db.update("""
			update course_refund_obligations set status='APPROVED',approved_amount_vnd=amount_vnd,
			 approved_by_account_id=?,approval_reason=?,approved_at=?,version=version+1,updated_at=?
			where id=? and status='PENDING_REVIEW' and version=?
			""", managerId, normalizedReason, nowTimestamp, nowTimestamp2, obligationId, expectedVersion);
	}

	int rejectUpdateCourseRefundObligations(UUID managerId, java.lang.String normalizedReason, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID obligationId, java.lang.Long expectedVersion) {
		return db.update("""
			update course_refund_obligations set status='REJECTED',rejected_by_account_id=?,rejection_reason=?,
			 rejected_at=?,version=version+1,updated_at=? where id=? and status='PENDING_REVIEW' and version=?
			""", managerId, normalizedReason, nowTimestamp, nowTimestamp2, obligationId, expectedVersion);
	}

	List<UUID> recordDisbursementFindCourseRefundObligations(UUID receptionistId, java.lang.String key) {
		return db.query("select id from course_refund_obligations where disbursed_by_account_id=? " + "and disbursement_idempotency_key=?", (rs, row) -> rs.getObject(1, UUID.class), receptionistId, key);
	}

	int recordDisbursementUpdateCourseRefundObligations(BigInteger amount, UUID receptionistId, Timestamp nowTimestamp, java.lang.String method, java.lang.String reference, java.lang.String evidence, java.lang.String key, Timestamp nowTimestamp2, UUID obligationId, long expectedVersion) {
		return db.update("""
			update course_refund_obligations set status='DISBURSEMENT_RECORDED',disbursed_amount_vnd=?,
			 disbursed_by_account_id=?,disbursed_at=?,disbursement_method=?,disbursement_reference=?,
			 evidence_reference=?,disbursement_idempotency_key=?,version=version+1,updated_at=?
			where id=? and status='APPROVED' and version=?
			""", amount, receptionistId, nowTimestamp, method, reference, evidence, key, nowTimestamp2, obligationId, expectedVersion);
	}

	int verifyUpdateCourseRefundObligations(Timestamp nowTimestamp, UUID obligationId, long version) {
		return db.update("update course_refund_obligations set status='PENDING_VERIFICATION'," + "version=version+1,updated_at=? where id=? and status='DISBURSEMENT_RECORDED' and version=?", nowTimestamp, obligationId, version);
	}

	int completeVerifiedRefund(BigInteger amount, UUID managerId, java.lang.String reason, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID obligationId, long version) {
		return db.update("""
			update course_refund_obligations set status='COMPLETED',verified_amount_vnd=?,
			 verified_by_account_id=?,verification_reason=?,verified_at=?,version=version+1,updated_at=?
			where id=? and status='PENDING_VERIFICATION' and version=?
			""", amount, managerId, reason, nowTimestamp, nowTimestamp2, obligationId, version);
	}

	int terminalUpdateCourseRefundObligations(java.lang.String target, UUID managerId, java.lang.String normalizedReason, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID obligationId, java.lang.String status, java.lang.Long expectedVersion) {
		return db.update("update course_refund_obligations set status=?,terminal_by_account_id=?," + "terminal_reason=?,terminal_at=?,version=version+1,updated_at=? where id=? and status=? and version=?", target, managerId, normalizedReason, nowTimestamp, nowTimestamp2, obligationId, status, expectedVersion);
	}

	List<Map<java.lang.String, java.lang.Object>> lockFindCourseRefundObligations(UUID obligationId) {
		return db.queryForList("select course_enrollment_id,course_run_id " + "from course_refund_obligations where id=?", obligationId);
	}

	java.lang.String lockFindCourseRuns(UUID runId) {
		return db.queryForObject("select status from course_runs where id=? for update", String.class, runId);
	}

	BigDecimal lockFindCourseEnrollments(UUID enrollmentId, UUID runId) {
		return db.queryForObject("select amount_paid_vnd_snapshot from course_enrollments " + "where id=? and course_run_id=? for update", BigDecimal.class, enrollmentId, runId);
	}

	List<com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation> lockFindOf(UUID enrollmentId) {
		return db.query(select() + " where obligation.course_enrollment_id=? order by obligation.id for update of obligation", this::map, enrollmentId);
	}

	java.lang.String findDisbursementIdempotencyKey(UUID obligationId) {
		return db.queryForObject("select disbursement_idempotency_key " + "from course_refund_obligations where id=?", String.class, obligationId);
	}
}
