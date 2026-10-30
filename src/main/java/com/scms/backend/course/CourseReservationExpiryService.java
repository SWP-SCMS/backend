package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseReservationExpiryService {

	private final JdbcTemplate db;
	private final AuditEventRepository audits;

	CourseReservationExpiryService(JdbcTemplate db, AuditEventRepository audits) {
		this.db = db;
		this.audits = audits;
	}

	List<UUID> findDueReservationIds(Instant now, UUID afterId, int limit) {
		return db.queryForList("""
			select id from course_reservations
			where status='HELD' and expires_at<=? and (?::uuid is null or id>?::uuid)
			order by id limit ?
			""", UUID.class, Timestamp.from(now), afterId, afterId, limit);
	}

	@Transactional
	boolean expireReservation(UUID reservationId, Instant now) {
		List<ExpiredReservation> expired = db.query("""
			update course_reservations
			set status='EXPIRED',updated_at=?
			where id=? and status='HELD' and expires_at<=?
			returning course_order_id,member_account_id,course_run_id
			""", (result, row) -> new ExpiredReservation(result.getObject(1, UUID.class),
			result.getObject(2, UUID.class), result.getObject(3, UUID.class)),
			Timestamp.from(now), reservationId, Timestamp.from(now));
		if (expired.isEmpty()) return false;
		ExpiredReservation reservation = expired.getFirst();
		db.update("""
			update course_orders set status='EXPIRED',updated_at=?
			where id=? and status='PENDING_PAYMENT'
			""", Timestamp.from(now), reservation.orderId());
		audits.save(new AuditEvent(UUID.randomUUID(), "COURSE_RESERVATION_EXPIRED", "COURSE_RESERVATION",
			reservationId, Map.of("orderId", reservation.orderId().toString(),
				"memberAccountId", reservation.memberId().toString(),
				"courseRunId", reservation.runId().toString(), "status", "EXPIRED")));
		return true;
	}

	void expireDueForRun(UUID runId, Instant now) {
		db.queryForList("""
			select id from course_reservations
			where course_run_id=? and status='HELD' and expires_at<=?
			order by id
			""", UUID.class, runId, Timestamp.from(now))
			.forEach(id -> expireReservation(id, now));
	}

	private record ExpiredReservation(UUID orderId, UUID memberId, UUID runId) { }
}
