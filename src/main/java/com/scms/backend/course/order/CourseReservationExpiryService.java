package com.scms.backend.course.order;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseReservationExpiryService {

	private final JdbcTemplate db;

	private final AuditEventRepository audits;

	CourseReservationExpiryService(JdbcTemplate db, AuditEventRepository audits) {
		this.db = db;
		this.audits = audits;
		this.courseReservationExpiryRepository = new CourseReservationExpiryRepository(this.db);
	}

	List<UUID> findDueReservationIds(Instant now, UUID afterId, int limit) {
		return courseReservationExpiryRepository.findDueReservationIdsFindCourseReservations(Timestamp.from(now), afterId, afterId, limit);
	}

	@Transactional
	public boolean expireReservation(UUID reservationId, Instant now) {
		List<Map<String, Object>> rows = courseReservationExpiryRepository.expireReservationFindCourseReservations(reservationId);
		if (rows.isEmpty())
			return false;
		Map<String, Object> row = rows.getFirst();
		ExpiredReservation reservation = new ExpiredReservation((UUID) row.get("course_order_id"), (UUID) row.get("member_account_id"), (UUID) row.get("course_run_id"));
		courseReservationExpiryRepository.expireReservationFindCourseRuns(reservation.runId());
		courseReservationExpiryRepository.expireReservationFindCourseOrders(reservation.orderId());
		courseReservationExpiryRepository.lockReservationForExpiry(reservationId);
		int expired = courseReservationExpiryRepository.expireReservationUpdateCourseReservations(Timestamp.from(now), reservationId, Timestamp.from(now));
		if (expired == 0)
			return false;
		courseReservationExpiryRepository.expireReservationUpdateCourseOrders(Timestamp.from(now), reservation.orderId());
		for (UUID paymentId : courseReservationExpiryRepository.expireReservationFindCoursePayments(reservation.orderId())) {
			courseReservationExpiryRepository.lockPaymentForExpiry(paymentId);
		}
		courseReservationExpiryRepository.expireReservationUpdateCoursePayments(Timestamp.from(now), reservation.orderId());
		audits.save(new AuditEvent(UUID.randomUUID(), "COURSE_RESERVATION_EXPIRED", "COURSE_RESERVATION", reservationId, Map.of("orderId", reservation.orderId().toString(), "memberAccountId", reservation.memberId().toString(), "courseRunId", reservation.runId().toString(), "status", "EXPIRED")));
		return true;
	}

	public void expireDueForRun(UUID runId, Instant now) {
		courseReservationExpiryRepository.expireDueForRunFindCourseReservations(runId, Timestamp.from(now)).forEach(id -> expireReservation(id, now));
	}

	private record ExpiredReservation(UUID orderId, UUID memberId, UUID runId) {
	}

	private final CourseReservationExpiryRepository courseReservationExpiryRepository;
}
