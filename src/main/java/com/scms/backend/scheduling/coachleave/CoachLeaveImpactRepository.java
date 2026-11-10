package com.scms.backend.scheduling.coachleave;

import com.scms.backend.scheduling.CoachLeaveStatus;
import com.scms.backend.scheduling.coachleave.CoachLeaveImpactService.AffectedSession;
import com.scms.backend.scheduling.coachleave.CoachLeaveImpactService.Impact;
import com.scms.backend.scheduling.session.AvailableCoachResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CoachLeaveImpactRepository {

	private final JdbcTemplate db;

	private final Clock clock;

	CoachLeaveImpactRepository(JdbcTemplate db, Clock clock) {
		this.db = db;
		this.clock = clock;
	}

	AffectedSession map(ResultSet rs, boolean pending) throws SQLException {
		Instant start = rs.getTimestamp("start_time").toInstant();
		String type = rs.getString("type"), status = rs.getString("status"), run = rs.getString("run_status");
		boolean course = type.equals("COURSE");
		boolean future = start.isAfter(clock.instant());
		boolean editable = pending && future && (status.equals("SCHEDULED") || (course && status.equals("MAKEUP_SCHEDULED"))) && (!course || !List.of("COMPLETED", "CANCELLED", "SETTLEMENT_PENDING").contains(run));
		boolean cancel = editable && rs.getLong("member_count") == 0 && (!course || (List.of("OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS").contains(run)));
		return new AffectedSession(rs.getObject("id", UUID.class), type, rs.getString("name"), rs.getObject("course_run_id", UUID.class), rs.getString("run_code"), (Integer) rs.getObject("unit_number"), start, rs.getTimestamp("end_time").toInstant(), rs.getObject("coach_id", UUID.class), rs.getString("coach_name"), rs.getObject("room_id", UUID.class), rs.getString("room_name"), rs.getInt("capacity"), rs.getLong("member_count"), status, rs.getLong("version"), editable, editable && (course || !clock.instant().isAfter(start.minus(Duration.ofHours(24)))), cancel);
	}

	Impact affectedSessionRows(CoachLeaveResponse request, List<UUID> ids) {
		// Use exactly the IDs used by approval, preserving the source type and Course Run context.
		String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
		String gym = """
			select s.id,case when c.class_type='PT_1_1' then 'PT' else 'CLASS' end type,
			  c.name,null::uuid course_run_id,null::text run_code,null::integer unit_number,
			  s.start_time,s.end_time,s.teaching_coach_account_id coach_id,a.full_name coach_name,
			  s.room_id,room.name room_name,s.capacity,s.status,s.version,
			  (select count(*) from bookings b where b.class_session_id=s.id and b.status='BOOKED') member_count,
			  null::text run_status,false is_makeup,null::uuid replacement_session_id
			from class_sessions s join sport_classes c on c.id=s.sport_class_id
			join accounts a on a.id=s.teaching_coach_account_id left join rooms room on room.id=s.room_id
			where s.id in (%s)
			""".formatted(placeholders);
		String course = """
			select s.id,'COURSE' type,c.name,r.id course_run_id,r.run_code,s.unit_number,
			  s.start_at start_time,s.end_at end_time,s.coach_account_id coach_id,a.full_name coach_name,
			  s.room_id,room.name room_name,r.capacity,s.status,s.version,
			  (select count(*) from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			    where es.course_session_id=s.id and es.disposition='ENROLLED' and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')) member_count,
			  r.status run_status,s.is_makeup,s.replacement_session_id
			from course_sessions s join course_runs r on r.id=s.course_run_id join courses c on c.id=r.course_id
			join accounts a on a.id=s.coach_account_id left join rooms room on room.id=s.room_id
			where s.id in (%s)
			""".formatted(placeholders);
		var args = new java.util.ArrayList<Object>(ids);
		args.addAll(ids);
		List<AffectedSession> sessions = db.query(gym + " union all " + course + " order by start_time,id,type", (rs, row) -> map(rs, request.status() == CoachLeaveStatus.PENDING), args.toArray());
		return new Impact(request, sessions);
	}

	List<AvailableCoachResponse> availableCoachRows(UUID excludedCoachId, AffectedSession session) {
		String type = session.type();
		UUID sessionId = session.id();
		String sql = """
			select a.id,a.full_name,a.phone from accounts a
			where a.role='COACH' and a.status='ACTIVE' and a.id<>?
			  and not exists(select 1 from class_sessions s where s.teaching_coach_account_id=a.id
			    and s.status in ('SCHEDULED','IN_PROGRESS') and s.start_time<? and s.end_time>?)
			  and not exists(select 1 from course_sessions s where s.coach_account_id=a.id
			    and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at<? and s.end_at>?)
			  and not exists(select 1 from coach_leave_requests l where l.coach_account_id=a.id and l.status='APPROVED'
			    and l.start_time<? and l.end_time>?)
			  and (?<>'PT' or not exists (
			    select 1 from (
			      select member_account_id member_id from bookings where class_session_id=? and status='BOOKED'
			      union select member_account_id member_id from session_waitlists where class_session_id=? and status='WAITING'
			    ) members where not exists(select 1 from coach_assignments ca where ca.member_account_id=members.member_id
			      and ca.coach_account_id=a.id and ca.status='ACTIVE')
			  )) order by a.full_name,a.id
			""";
		return db.query(sql, (rs, row) -> new AvailableCoachResponse(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("phone")), excludedCoachId, java.sql.Timestamp.from(session.endTime()), java.sql.Timestamp.from(session.startTime()), java.sql.Timestamp.from(session.endTime()), java.sql.Timestamp.from(session.startTime()), java.sql.Timestamp.from(session.endTime()), java.sql.Timestamp.from(session.startTime()), type, sessionId, sessionId);
	}
}
