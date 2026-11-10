package com.scms.backend.course.refund;

import static com.scms.backend.course.refund.CourseRefundApiModels.ReceptionCourseRefund;
import static com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation;
import com.scms.backend.course.CourseRefundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
			 select refund.id refund_id,refund.member_account_id member_id,profile.member_code,member.full_name member_name,
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
}
