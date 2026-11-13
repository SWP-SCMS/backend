package com.scms.backend.course.enrollment;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseAttendanceException;
import com.scms.backend.course.CourseAttendanceStatus;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.AttendanceUpdate;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.MemberEnrollment;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.MemberPaymentSummary;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.MemberSession;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.RosterEntry;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChangeResponse;
import com.scms.backend.course.schedulechange.CourseScheduleChangeService;
import com.scms.backend.course.session.CourseAttendancePolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseEnrollmentService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final Clock clock;

	private final CourseScheduleChangeService scheduleChanges;

	public CourseEnrollmentService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, Clock clock, CourseScheduleChangeService scheduleChanges) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
		this.scheduleChanges = scheduleChanges;
		this.courseEnrollmentRepository = new CourseEnrollmentRepository(this.db);
	}

	public List<ScheduleChange> scheduleChanges(UUID memberId) {
		return scheduleChanges.list(memberId);
	}

	public ScheduleChange respondToScheduleChange(UUID memberId, UUID responseId, ScheduleChangeResponse request) {
		return scheduleChanges.respond(memberId, responseId, request == null ? null : request.response());
	}

	@Transactional(readOnly = true)
	public List<MemberEnrollment> memberEnrollments(UUID memberId) {
		active(memberId, AccountRole.MEMBER);
		List<Map<String, Object>> rows = courseEnrollmentRepository.memberEnrollmentsFindCourseEnrollments(memberId);
		Map<UUID, EnrollmentBuilder> grouped = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			UUID id = uuid(row, "enrollment_id");
			grouped.computeIfAbsent(id, ignored -> new EnrollmentBuilder(id, uuid(row, "course_run_id"), text(row, "course_code"), text(row, "course_name"), text(row, "run_code"), CourseRunStatus.valueOf(text(row, "course_run_status")), text(row, "lifecycle_status"), instant(row, "enrolled_at"), new MemberPaymentSummary(amount(row, "amount_paid_vnd_snapshot"), text(row, "payment_status"), text(row, "payment_method")))).sessions.add(new MemberSession(uuid(row, "enrollment_session_id"), uuid(row, "course_session_id"), number(row, "unit_number"), instant(row, "start_at"), instant(row, "end_at"), text(row, "disposition"), CourseSessionStatus.valueOf(text(row, "session_status")), CourseAttendanceStatus.valueOf(text(row, "attendance_status")), instant(row, "attendance_recorded_at"), Boolean.TRUE.equals(row.get("is_makeup")), uuid(row, "original_session_id"), uuid(row, "replacement_session_id"), text(row, "room_name"), text(row, "coach_name")));
		}
		return grouped.values().stream().map(EnrollmentBuilder::response).toList();
	}

	@Transactional(readOnly = true)
	public List<RosterEntry> roster(UUID coachId, UUID sessionId) {
		active(coachId, AccountRole.COACH);
		List<RosterEntry> result = courseEnrollmentRepository.rosterFindCourseEnrollmentSessions(coachId, sessionId, Timestamp.from(clock.instant()));
		if (result.isEmpty() && courseEnrollmentRepository.rosterFindCourseSessions(sessionId, coachId) != 1)
			throw CourseAttendanceException.notFound();
		return result;
	}

	@Transactional
	public RosterEntry updateAttendance(UUID coachId, UUID sessionId, UUID enrollmentSessionId, AttendanceUpdate request) {
		active(coachId, AccountRole.COACH);
		if (request == null || request.status() == null || !CourseAttendanceStatus.writableValues().contains(request.status()))
			throw CourseAttendanceException.invalid();
		Map<String, Object> session = courseEnrollmentRepository.updateAttendanceFindCourseRuns(sessionId, sessionId, coachId).stream().findFirst().orElseThrow(CourseAttendanceException::notFound);
		Instant now = clock.instant();
		CourseAttendancePolicy.Decision attendance = CourseAttendancePolicy.evaluate(text(session, "status"), text(session, "run_status"), instant(session, "start_at"), instant(session, "end_at"), now, true);
		if (!attendance.writable())
			throw CourseAttendanceException.closed();
		Map<String, Object> row = courseEnrollmentRepository.updateAttendanceFindCourseEnrollmentSessions(enrollmentSessionId, sessionId, Timestamp.from(now)).stream().findFirst().orElseThrow(CourseAttendanceException::notFound);
		if (!Boolean.TRUE.equals(row.get("checked_in")))
			throw com.scms.backend.scheduling.AttendanceException.visit();
		CourseAttendanceStatus before = CourseAttendanceStatus.valueOf(text(row, "attendance_status"));
		courseEnrollmentRepository.updateAttendanceUpdateCourseEnrollmentSessions(request.status().name(), Timestamp.from(now), coachId, Timestamp.from(now), enrollmentSessionId);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "COURSE_ATTENDANCE_CHANGED", "CourseEnrollmentSession", enrollmentSessionId, "Coach changed Course attendance", Map.of("status", before.name()), Map.of("status", request.status().name(), "courseSessionId", sessionId.toString())));
		return new RosterEntry(enrollmentSessionId, text(row, "member_code"), text(row, "full_name"), text(row, "profile_image_url"), request.status(), instant(row, "attendance_recorded_at") == null ? now : instant(row, "attendance_recorded_at"), uuid(row, "member_account_id"), true);
	}

	private void active(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private static UUID uuid(Map<String, Object> row, String key) {
		return (UUID) row.get(key);
	}

	private static String text(Map<String, Object> row, String key) {
		return row.get(key) == null ? null : row.get(key).toString();
	}

	private static int number(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).intValue();
	}

	private static BigInteger amount(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? null : value instanceof BigDecimal decimal ? decimal.toBigIntegerExact() : BigInteger.valueOf(((Number) value).longValue());
	}

	private static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? null : value instanceof Instant instant ? instant : ((Timestamp) value).toInstant();
	}

	private record EnrollmentBuilder(UUID id, UUID runId, String courseCode, String courseName, String runCode, CourseRunStatus courseRunStatus, String lifecycleStatus, Instant enrolledAt, MemberPaymentSummary payment, List<MemberSession> sessions) {

		EnrollmentBuilder(UUID id, UUID runId, String courseCode, String courseName, String runCode, CourseRunStatus courseRunStatus, String lifecycleStatus, Instant enrolledAt, MemberPaymentSummary payment) {
			this(id, runId, courseCode, courseName, runCode, courseRunStatus, lifecycleStatus, enrolledAt, payment, new ArrayList<>());
		}

		MemberEnrollment response() {
			return new MemberEnrollment(id, runId, courseCode, courseName, runCode, courseRunStatus, lifecycleStatus, enrolledAt, payment, List.copyOf(sessions), "WITHDRAWAL_PENDING".equals(lifecycleStatus));
		}
	}

	private final CourseEnrollmentRepository courseEnrollmentRepository;
}
