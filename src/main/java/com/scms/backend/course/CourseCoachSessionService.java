package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseCoachSessionApiModels.CourseCoachSessionPage;
import com.scms.backend.course.CourseCoachSessionApiModels.CourseCoachSessionResponse;
import com.scms.backend.course.CourseCoachSessionApiModels.AttendanceWriteReason;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseCoachSessionService {
	private static final String FROM = """
		from course_sessions s
		join course_runs cr on cr.id=s.course_run_id
		join courses c on c.id=cr.course_id
		join accounts coach on coach.id=s.coach_account_id
		join rooms room on room.id=s.room_id
		left join course_enrollment_sessions es on es.course_session_id=s.id
		left join course_enrollments e on e.id=es.course_enrollment_id
		""";
	private static final String SELECT = """
		select s.id course_session_id,s.unit_number,s.start_at starts_at,s.end_at ends_at,
			c.id course_id,c.code course_code,c.name course_name,
			cr.id course_run_id,cr.run_code,cr.status course_run_status,
			coach.id coach_id,coach.full_name coach_name,
			room.id room_id,room.name room_name,s.status session_status,
			count(es.id) filter (where es.disposition='ENROLLED'
				and e.lifecycle_status='ACTIVE') roster_count
		""";
	private static final String GROUP_BY = """
		 group by s.id,s.unit_number,s.start_at,s.end_at,c.id,c.code,c.name,
			cr.id,cr.run_code,cr.status,coach.id,coach.full_name,room.id,room.name,s.status
		""";

	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final Clock clock;

	CourseCoachSessionService(JdbcTemplate db, AccountRepository accounts, Clock clock) {
		this.db = db; this.accounts = accounts; this.clock = clock;
	}

	@Transactional(readOnly = true)
	CourseCoachSessionPage list(UUID coachId, Instant from, Instant to, String status, int page, int size,
			List<String> sort) {
		validate(coachId, from, to, status, page, size);
		Query query = query(from, to, status);
		List<Object> filterArgs = new ArrayList<>();
		filterArgs.add(coachId); filterArgs.addAll(query.args);
		long total = db.queryForObject("select count(*) from course_sessions s join course_runs cr on cr.id=s.course_run_id "
			+ "where s.coach_account_id=?" + query.where, Long.class, filterArgs.toArray());
		List<Object> args = new ArrayList<>(filterArgs);
		args.add(size); args.add((long) page * size);
		List<CourseCoachSessionResponse> content = db.query(SELECT + FROM + "where s.coach_account_id=?" + query.where
			+ GROUP_BY + orderBy(sort) + " limit ? offset ?", (rs, row) -> map(rs), args.toArray());
		return new CourseCoachSessionPage(content, page, size, total, (int) ((total + size - 1) / size));
	}

	@Transactional(readOnly = true)
	CourseCoachSessionResponse detail(UUID coachId, UUID sessionId) {
		active(coachId);
		List<CourseCoachSessionResponse> rows = db.query(SELECT + FROM
			+ "where s.coach_account_id=? and s.id=?" + GROUP_BY, (rs, row) -> map(rs), coachId, sessionId);
		if (rows.isEmpty()) throw CourseException.sessionNotFound();
		return rows.get(0);
	}

	private Query query(Instant from, Instant to, String status) {
		StringBuilder where = new StringBuilder();
		List<Object> args = new ArrayList<>();
		if (from != null) { where.append(" and s.end_at > ?"); args.add(Timestamp.from(from)); }
		if (to != null) { where.append(" and s.start_at < ?"); args.add(Timestamp.from(to)); }
		if (status != null) { where.append(" and s.status=?"); args.add(status); }
		return new Query(where.toString(), args);
	}

	private String orderBy(List<String> sorts) {
		List<String> order = new ArrayList<>();
		if (sorts != null) for (String value : sorts) {
			String[] parts = value.split(",", -1);
			String property = parts[0];
			String column = switch (property) {
				case "startsAt" -> "s.start_at";
				case "endsAt" -> "s.end_at";
				case "unitNumber" -> "s.unit_number";
				case "sessionStatus" -> "s.status";
				case "id", "courseSessionId" -> "s.id";
				default -> throw CourseException.validation("sort", "must use a supported CourseSession field");
			};
			String direction = parts.length > 1 ? parts[1].toLowerCase() : "asc";
			if (!direction.equals("asc") && !direction.equals("desc"))
				throw CourseException.validation("sort", "direction must be asc or desc");
			order.add(column + " " + direction);
		}
		if (order.isEmpty()) order.add("s.start_at asc");
		if (order.stream().noneMatch(value -> value.startsWith("s.id "))) order.add("s.id asc");
		return " order by " + String.join(",", order);
	}

	private void validate(UUID coachId, Instant from, Instant to, String status, int page, int size) {
		active(coachId);
		if (from != null && to != null && !from.isBefore(to)) throw CourseException.validation("from", "must be before to");
		if (status != null) try { CourseSessionStatus.valueOf(status); }
		catch (IllegalArgumentException exception) { throw CourseException.validation("status", "must be a valid CourseSession status"); }
		if (page < 0) throw CourseException.validation("page", "must be greater than or equal to zero");
		if (size < 1 || size > 100) throw CourseException.validation("size", "must be between 1 and 100");
	}

	private void active(UUID coachId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private CourseCoachSessionResponse map(java.sql.ResultSet rs) throws java.sql.SQLException {
		Instant startsAt = rs.getTimestamp("starts_at").toInstant();
		Instant endsAt = rs.getTimestamp("ends_at").toInstant();
		CourseAttendancePolicy.Decision decision = CourseAttendancePolicy.evaluate(rs.getString("session_status"),
			rs.getString("course_run_status"), startsAt, endsAt, clock.instant(), true);
		return new CourseCoachSessionResponse(rs.getObject("course_session_id", UUID.class), rs.getInt("unit_number"),
			startsAt, endsAt, rs.getObject("course_id", UUID.class), rs.getString("course_code"),
			rs.getString("course_name"), rs.getObject("course_run_id", UUID.class), rs.getString("run_code"),
			CourseRunStatus.valueOf(rs.getString("course_run_status")), rs.getObject("coach_id", UUID.class),
			rs.getString("coach_name"), rs.getObject("room_id", UUID.class), null, rs.getString("room_name"),
			CourseSessionStatus.valueOf(rs.getString("session_status")), rs.getLong("roster_count"),
			decision.writable(), AttendanceWriteReason.valueOf(decision.reason()));
	}

	private record Query(String where, List<Object> args) {}
}
