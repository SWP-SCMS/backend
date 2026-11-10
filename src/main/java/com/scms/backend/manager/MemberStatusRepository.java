package com.scms.backend.manager;

import com.scms.backend.manager.MemberStatusService.CancelledBooking;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class MemberStatusRepository {

	List<CancelledBooking> lockFutureBookings(UUID memberId, Timestamp nowTimestamp) {
		return jdbcTemplate.query("""
			select b.id, b.class_session_id from bookings b
			join class_sessions s on s.id=b.class_session_id
			where b.member_account_id=? and b.status='BOOKED' and s.start_time > ?
			for update of b
			""", (rs, row) -> new CancelledBooking(rs.getObject("id", UUID.class), rs.getObject("class_session_id", UUID.class)), memberId, nowTimestamp);
	}

	int cancelFutureBookings(UUID managerId, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID memberId, Timestamp nowTimestamp3) {
		return jdbcTemplate.update("""
			update bookings b
			set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'MEMBER_SUSPENDED', cancelled_at = ?, updated_at = ?, version = b.version + 1
			from class_sessions s
			where b.class_session_id = s.id and b.member_account_id = ?
				and b.status = 'BOOKED' and s.start_time > ?
			""", managerId, nowTimestamp, nowTimestamp2, memberId, nowTimestamp3);
	}

	private final JdbcTemplate jdbcTemplate;

	MemberStatusRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
