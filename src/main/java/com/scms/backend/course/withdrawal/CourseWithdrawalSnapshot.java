package com.scms.backend.course.withdrawal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import com.scms.backend.course.withdrawal.CourseWithdrawalService.RefundSnapshot;

final class CourseWithdrawalSnapshot {

	private final JdbcTemplate db;

	CourseWithdrawalSnapshot(JdbcTemplate db) {
		this.db = db;
	}

	RefundSnapshot refundSnapshot(UUID enrollmentId, int totalUnits, BigInteger amountPaid, Instant now) {
		List<Map<String, Object>> units = db.queryForList("""
			select s.start_at,es.allocation_vnd from course_enrollment_sessions es
			join course_sessions s on s.id=es.course_session_id
			where es.course_enrollment_id=? and not exists (select 1 from course_refund_unit_claims c where c.enrollment_session_id=es.id) order by es.unit_number for update of es
			""", enrollmentId);
		int remaining = 0;
		BigInteger unused = BigInteger.ZERO;
		for (Map<String, Object> unit : units) if (instant(unit, "start_at").isAfter(now)) {
			remaining++;
			unused = unused.add(amount(unit, "allocation_vnd"));
		}
		return new RefundSnapshot(totalUnits, remaining, unused.min(amountPaid));
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return CourseWithdrawalRepository.amount(row, key);
	}

	static BigInteger amount(BigDecimal value) {
		return CourseWithdrawalRepository.amount(value);
	}

	static Instant instant(Map<String, Object> row, String key) {
		return CourseWithdrawalRepository.instant(row, key);
	}

	static Instant instant(Timestamp value) {
		return CourseWithdrawalRepository.instant(value);
	}
}
