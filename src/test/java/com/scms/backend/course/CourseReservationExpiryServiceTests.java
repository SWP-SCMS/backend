package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;

class CourseReservationExpiryServiceTests {

	@Test
	void expiryLocksRunThenOrderThenReservationBeforeUpdating() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		UUID reservationId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID runId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-30T00:00:00Z");
		when(db.queryForList(anyString(), eq(reservationId))).thenReturn(List.of(Map.of(
			"course_order_id", orderId, "member_account_id", memberId, "course_run_id", runId)));
		when(db.queryForList("select id from course_payments where course_order_id=? order by id",
			UUID.class, orderId)).thenReturn(List.of(paymentId));
		when(db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId))
			.thenReturn(runId);
		when(db.queryForObject("select id from course_orders where id=? for update", UUID.class, orderId))
			.thenReturn(orderId);
		when(db.queryForObject("select id from course_reservations where id=? for update", UUID.class, reservationId))
			.thenReturn(reservationId);
		when(db.queryForObject("select id from course_payments where id=? for update", UUID.class, paymentId))
			.thenReturn(paymentId);
		when(db.update(anyString(), any(Object[].class))).thenReturn(1);
		when(db.update("update course_payments set status='FAILED',updated_at=? "
				+ "where course_order_id=? and status='PENDING'", Timestamp.from(now), orderId)).thenReturn(1);
		CourseReservationExpiryService service = new CourseReservationExpiryService(db,
			mock(AuditEventRepository.class));

		assertThat(service.expireReservation(reservationId, now)).isTrue();

		InOrder locks = inOrder(db);
		locks.verify(db).queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
		locks.verify(db).queryForObject("select id from course_orders where id=? for update", UUID.class, orderId);
		locks.verify(db).queryForObject("select id from course_reservations where id=? for update", UUID.class,
			reservationId);
		locks.verify(db).update(anyString(), eq(Timestamp.from(now)), eq(reservationId), eq(Timestamp.from(now)));
		locks.verify(db).queryForObject("select id from course_payments where id=? for update", UUID.class, paymentId);
		locks.verify(db).update("update course_payments set status='FAILED',updated_at=? "
			+ "where course_order_id=? and status='PENDING'", Timestamp.from(now), orderId);
	}
}
