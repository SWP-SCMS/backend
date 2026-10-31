package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseEnrollmentApiModels.AttendanceUpdate;
import com.scms.backend.course.CourseEnrollmentApiModels.MemberEnrollment;
import com.scms.backend.course.CourseEnrollmentApiModels.MemberSession;
import com.scms.backend.course.CourseEnrollmentApiModels.RosterEntry;
import com.scms.backend.course.CourseScheduleChangeApiModels.ScheduleChange;
import com.scms.backend.course.CourseScheduleChangeApiModels.ScheduleChangeResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseEnrollmentService {
	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;
	private final CourseScheduleChangeService scheduleChanges;

	CourseEnrollmentService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, Clock clock,
			CourseScheduleChangeService scheduleChanges) {
		this.db = db; this.accounts = accounts; this.audits = audits; this.clock = clock; this.scheduleChanges = scheduleChanges;
	}

	List<ScheduleChange> scheduleChanges(UUID memberId) { return scheduleChanges.list(memberId); }
	ScheduleChange respondToScheduleChange(UUID memberId, UUID responseId, ScheduleChangeResponse request) {
		return scheduleChanges.respond(memberId, responseId, request == null ? null : request.response());
	}

	@Transactional(readOnly = true)
	List<MemberEnrollment> memberEnrollments(UUID memberId) {
		active(memberId, AccountRole.MEMBER);
		List<Map<String, Object>> rows = db.queryForList("""
			select e.id enrollment_id,e.course_run_id,c.code course_code,c.name course_name,r.run_code,e.lifecycle_status,e.enrolled_at,
				es.id enrollment_session_id,es.course_session_id,es.unit_number,s.start_at,s.end_at,
				es.disposition,es.attendance_status,es.attendance_recorded_at
			from course_enrollments e join course_runs r on r.id=e.course_run_id join courses c on c.id=r.course_id
			join course_enrollment_sessions es on es.course_enrollment_id=e.id
			join course_sessions s on s.id=es.course_session_id
			where e.member_account_id=? order by e.enrolled_at desc,e.id,es.unit_number
			""", memberId);
		Map<UUID, EnrollmentBuilder> grouped = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			UUID id = uuid(row, "enrollment_id");
			grouped.computeIfAbsent(id, ignored -> new EnrollmentBuilder(id, uuid(row, "course_run_id"),
				text(row, "course_code"), text(row, "course_name"), text(row, "run_code"),
				text(row, "lifecycle_status"), instant(row, "enrolled_at"))).sessions.add(new MemberSession(uuid(row, "enrollment_session_id"),
				uuid(row, "course_session_id"), number(row, "unit_number"), instant(row, "start_at"),
				instant(row, "end_at"), text(row, "disposition"),
				CourseAttendanceStatus.valueOf(text(row, "attendance_status")), instant(row, "attendance_recorded_at")));
		}
		return grouped.values().stream().map(EnrollmentBuilder::response).toList();
	}

	@Transactional(readOnly = true)
	List<RosterEntry> roster(UUID coachId, UUID sessionId) {
		active(coachId, AccountRole.COACH);
		List<RosterEntry> result = db.query("""
			select es.id,e.member_account_id,p.member_code,a.full_name,p.profile_image_url,
				es.attendance_status,es.attendance_recorded_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join course_sessions s on s.id=es.course_session_id and s.coach_account_id=?
			join accounts a on a.id=e.member_account_id join member_profiles p on p.account_id=a.id
			where es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by a.full_name,a.id
			""", (rs, row) -> new RosterEntry(rs.getObject("id", UUID.class),
			rs.getString("member_code"), rs.getString("full_name"),
			rs.getString("profile_image_url"), CourseAttendanceStatus.valueOf(rs.getString("attendance_status")),
			rs.getTimestamp("attendance_recorded_at") == null ? null : rs.getTimestamp("attendance_recorded_at").toInstant()),
			coachId, sessionId);
		if (result.isEmpty() && db.queryForObject("select count(*) from course_sessions where id=? and coach_account_id=?",
			Long.class, sessionId, coachId) != 1) throw CourseAttendanceException.notFound();
		return result;
	}

	@Transactional
	RosterEntry updateAttendance(UUID coachId, UUID sessionId, UUID enrollmentSessionId, AttendanceUpdate request) {
		active(coachId, AccountRole.COACH);
		if (request == null || request.status() == null || !CourseAttendanceStatus.writableValues().contains(request.status()))
			throw CourseAttendanceException.invalid();
		Map<String, Object> session = db.queryForList("""
			select id,start_at,end_at,status from course_sessions
			where id=? and coach_account_id=? for update
			""", sessionId, coachId).stream().findFirst().orElseThrow(CourseAttendanceException::notFound);
		Instant now = clock.instant();
		if (now.isBefore(instant(session, "start_at")) || !now.isBefore(instant(session, "end_at").plusSeconds(1800))
				|| !(text(session, "status").equals("SCHEDULED") || text(session, "status").equals("MAKEUP_SCHEDULED")))
			throw CourseAttendanceException.closed();
		Map<String, Object> row = db.queryForList("""
			select es.id,e.member_account_id,p.member_code,a.full_name,p.profile_image_url,
				es.attendance_status,es.attendance_recorded_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join accounts a on a.id=e.member_account_id join member_profiles p on p.account_id=a.id
			where es.id=? and es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') for update of es
			""", enrollmentSessionId, sessionId).stream().findFirst().orElseThrow(CourseAttendanceException::notFound);
		CourseAttendanceStatus before = CourseAttendanceStatus.valueOf(text(row, "attendance_status"));
		db.update("""
			update course_enrollment_sessions set attendance_status=?,attendance_recorded_at=coalesce(attendance_recorded_at,?),
			attendance_recorded_by=?,attendance_updated_at=?,version=version+1 where id=?
			""", request.status().name(), Timestamp.from(now), coachId, Timestamp.from(now), enrollmentSessionId);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "COURSE_ATTENDANCE_CHANGED", "CourseEnrollmentSession",
			enrollmentSessionId, "Coach changed Course attendance", Map.of("status", before.name()),
			Map.of("status", request.status().name(), "courseSessionId", sessionId.toString())));
		return new RosterEntry(enrollmentSessionId, text(row, "member_code"),
			text(row, "full_name"), text(row, "profile_image_url"), request.status(),
			instant(row, "attendance_recorded_at") == null ? now : instant(row, "attendance_recorded_at"));
	}

	private void active(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}
	private static UUID uuid(Map<String, Object> row, String key) { return (UUID) row.get(key); }
	private static String text(Map<String, Object> row, String key) { return row.get(key) == null ? null : row.get(key).toString(); }
	private static int number(Map<String, Object> row, String key) { return ((Number) row.get(key)).intValue(); }
	private static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key); return value == null ? null : value instanceof Instant instant ? instant : ((Timestamp) value).toInstant();
	}
	private record EnrollmentBuilder(UUID id, UUID runId, String courseCode, String courseName, String runCode, String lifecycleStatus,
			Instant enrolledAt, List<MemberSession> sessions) {
		EnrollmentBuilder(UUID id, UUID runId, String courseCode, String courseName, String runCode, String lifecycleStatus, Instant enrolledAt) {
			this(id, runId, courseCode, courseName, runCode, lifecycleStatus, enrolledAt, new ArrayList<>());
		}
		MemberEnrollment response() { return new MemberEnrollment(id, runId, courseCode, courseName, runCode,
			lifecycleStatus, enrolledAt, List.copyOf(sessions)); }
	}
}
