package com.scms.backend.course.withdrawal;

import com.scms.backend.course.withdrawal.CourseWithdrawalService.RefundSnapshot;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseWithdrawalSnapshot {

	private final JdbcTemplate db;

	CourseWithdrawalSnapshot(JdbcTemplate db) {
		this.db = db;
		this.courseWithdrawalRepository = new CourseWithdrawalRepository(this.db);
	}

	RefundSnapshot refundSnapshot(UUID enrollmentId, int totalUnits, BigInteger amountPaid, Instant now) {
		List<Map<String, Object>> units = courseWithdrawalRepository.refundSnapshotFindCourseEnrollmentSessions(enrollmentId);
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

	private final CourseWithdrawalRepository courseWithdrawalRepository;
}
