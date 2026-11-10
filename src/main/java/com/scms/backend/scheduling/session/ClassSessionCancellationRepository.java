package com.scms.backend.scheduling.session;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class ClassSessionCancellationRepository {

	List<UUID> cancelLockedFindBookings(UUID sessionId) {
		return jdbc.queryForList("""
			select member_account_id from bookings
			where class_session_id = ? and status = 'BOOKED'
			for update
			""", UUID.class, sessionId);
	}

	int cancelLockedUpdateBookings(UUID managerId, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID sessionId) {
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
