package com.scms.backend.course.session;

import com.scms.backend.course.session.CourseResourceAvailabilityController.Occupied;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseResourceAvailabilityRepository {

	List<Occupied> availableFindClassSessions(List<UUID> excluded, Timestamp endTimestamp, Timestamp startTimestamp) {
		String exclusion = excluded.isEmpty() ? "" : "and id not in (" + String.join(",", java.util.Collections.nCopies(excluded.size(), "?")) + ")";
		var args = new java.util.ArrayList<Object>(excluded);
		args.add(endTimestamp);
		args.add(startTimestamp);
		return db.query("""
			select coach_id, room_id, start_at, end_at from (
			    select teaching_coach_account_id coach_id, room_id, start_time start_at, end_time end_at
			    from class_sessions where status in ('SCHEDULED','IN_PROGRESS')
			    union all
			    select coach_account_id, room_id, start_at, end_at from course_sessions
			    where status in ('SCHEDULED','MAKEUP_SCHEDULED')
			      %s
			    union all
			    select coach_account_id, null::uuid, start_time, end_time from coach_leave_requests where status='APPROVED'
			    union all
			    select null::uuid, room_id, start_time, end_time from room_suspensions
			) busy where start_at < ? and end_at > ?
			""".formatted(exclusion), (rs, row) -> new Occupied(rs.getObject("coach_id", UUID.class), rs.getObject("room_id", UUID.class), rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant()), args.toArray());
	}

	private final JdbcTemplate db;

	CourseResourceAvailabilityRepository(JdbcTemplate db) {
		this.db = db;
	}
}
