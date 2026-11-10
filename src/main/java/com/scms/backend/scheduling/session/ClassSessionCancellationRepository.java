package com.scms.backend.scheduling.session;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class ClassSessionCancellationRepository {

	java.util.List<java.util.UUID> cancelLockedFindBookings(java.util.UUID sessionId) {
		return jdbc.queryForList("""
			select member_account_id from bookings
			where class_session_id = ? and status = 'BOOKED'
			for update
			""", UUID.class, sessionId);
	}

	int cancelLockedUpdateBookings(java.util.UUID managerId, java.sql.Timestamp nowTimestamp, java.sql.Timestamp nowTimestamp2, java.util.UUID sessionId) {
		return jdbc.update("""
			update bookings set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'SESSION_CANCELLED', cancelled_at = ?, updated_at = ?, version = version + 1
			where class_session_id = ? and status = 'BOOKED'
			""", managerId, nowTimestamp, nowTimestamp2, sessionId);
	}

	private final JdbcTemplate jdbc;

	ClassSessionCancellationRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
}
