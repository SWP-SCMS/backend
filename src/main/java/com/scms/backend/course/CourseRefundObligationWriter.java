package com.scms.backend.course;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseRefundObligationWriter {
	private static final BigInteger MAX_VND = new BigInteger("9999999999999999999");
	private final JdbcTemplate db;

	CourseRefundObligationWriter(JdbcTemplate db) { this.db = db; }

	@Transactional
	UUID create(UUID enrollmentId, UUID runId, CourseRefundSource source, BigInteger amount, UUID requesterId,
			String reason) {
		if (enrollmentId == null || runId == null || source == null || amount == null || amount.signum() < 0
				|| amount.compareTo(MAX_VND) > 0 || reason == null || reason.isBlank()
				|| reason.trim().length() > 1000) {
			throw CourseRefundException.invalid("Valid source, VND amount, and reason are required.");
		}
		db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
		Map<String, Object> enrollment = db.queryForList("select id,course_run_id from course_enrollments "
			+ "where id=? and course_run_id=? for update", enrollmentId, runId).stream().findFirst()
			.orElseThrow(CourseRefundException::notFound);
		UUID id = UUID.randomUUID();
		db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,
			 requester_account_id,request_reason) values(?,?,?,?,?,?,?)
			on conflict(course_enrollment_id,source_type) do nothing
			""", id, enrollment.get("id"), runId, source.name(), amount, requesterId, reason.trim());
		Map<String, Object> stored = db.queryForMap("select id,course_run_id,amount_vnd from course_refund_obligations "
			+ "where course_enrollment_id=? and source_type=? for update", enrollmentId, source.name());
		BigInteger storedAmount = new BigDecimal(stored.get("amount_vnd").toString()).toBigIntegerExact();
		if (!runId.equals(stored.get("course_run_id")) || !amount.equals(storedAmount)) {
			throw CourseRefundException.conflict("COURSE_REFUND_SOURCE_CONFLICT",
				"The source already created a different refund obligation.");
		}
		return (UUID) stored.get("id");
	}
}
