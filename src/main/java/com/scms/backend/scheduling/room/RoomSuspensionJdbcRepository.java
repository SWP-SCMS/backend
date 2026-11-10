package com.scms.backend.scheduling.room;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class RoomSuspensionJdbcRepository {

	List<UUID> notifyChangedSessionFindBookings(UUID sessionId) {
		return jdbc.queryForList("""
			select member_account_id from bookings
			where class_session_id = ? and status = 'BOOKED'
			""", UUID.class, sessionId);
	}

	private final JdbcTemplate jdbc;

	RoomSuspensionJdbcRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
}
