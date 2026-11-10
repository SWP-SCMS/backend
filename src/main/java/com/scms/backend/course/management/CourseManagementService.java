package com.scms.backend.course.management;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.Course;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.VersionRequest;
import com.scms.backend.course.run.CourseRunClosureService;
import com.scms.backend.course.run.CourseRunCreateRequest;
import com.scms.backend.course.run.CourseRunOpenRequest;
import com.scms.backend.course.run.CourseRunPageResponse;
import com.scms.backend.course.run.CourseRunPatchRequest;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.course.run.CourseRunResponse;
import com.scms.backend.course.run.CourseRunScheduleRequest;
import com.scms.backend.course.schedulechange.CourseScheduleChangeService;
import com.scms.backend.course.session.CourseSessionCreateRequest;
import com.scms.backend.course.session.CourseSessionPatchRequest;
import com.scms.backend.course.session.CourseSessionRepository;
import com.scms.backend.course.session.CourseSessionResponse;
import com.scms.backend.course.session.CourseSessionService;
import com.scms.backend.scheduling.discipline.DisciplineRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseManagementService {

	private final CourseRepository courses;

	private final CourseRunRepository runs;

	private final CourseSessionRepository sessions;

	private final DisciplineRepository disciplines;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final CourseSessionService sessionService;

	private final CourseRunClosureService runClosure;

	private final JdbcTemplate db;

	private final CourseScheduleChangeService scheduleChanges;

	private final Clock clock;

	CourseManagementService(CourseRepository courses, CourseRunRepository runs, CourseSessionRepository sessions, DisciplineRepository disciplines, AccountRepository accounts, AuditEventRepository audits, CourseSessionService sessionService, CourseRunClosureService runClosure) {
		this(courses, runs, sessions, disciplines, accounts, audits, sessionService, runClosure, null, null, Clock.systemUTC());
	}

	@org.springframework.beans.factory.annotation.Autowired
	CourseManagementService(CourseRepository courses, CourseRunRepository runs, CourseSessionRepository sessions, DisciplineRepository disciplines, AccountRepository accounts, AuditEventRepository audits, CourseSessionService sessionService, CourseRunClosureService runClosure, JdbcTemplate db, CourseScheduleChangeService scheduleChanges, Clock clock) {
		this.courses = courses;
		this.runs = runs;
		this.sessions = sessions;
		this.disciplines = disciplines;
		this.accounts = accounts;
		this.audits = audits;
		this.sessionService = sessionService;
		this.runClosure = runClosure;
		this.db = db;
		this.scheduleChanges = scheduleChanges;
		this.clock = clock;
		this.courseAdministration = new CourseAdministration(this.courses, this.runs, this.disciplines, this.accounts, this.audits, this.runClosure, this.clock);
		this.courseRunAdministration = new CourseRunAdministration(this.runs, this.sessions, this.accounts, this.runClosure, this.db, this.scheduleChanges, this.clock, this.courseAdministration);
		this.courseManagementRepository = new CourseManagementRepository(this.db);
	}

	@Transactional(readOnly = true)
	CoursePageResponse listCourses(UUID managerId, Pageable pageable) {
		return courseAdministration.listCourses(managerId, pageable);
	}

	@Transactional(readOnly = true)
	public CourseResponse getCourse(UUID managerId, UUID courseId) {
		return courseAdministration.getCourse(managerId, courseId);
	}

	@Transactional
	public CourseResponse createCourse(UUID managerId, CourseCreateRequest request) {
		return courseAdministration.createCourse(managerId, request);
	}

	@Transactional(readOnly = true)
	CourseApprovalPageResponse listCourseApprovals(UUID managerId, Pageable pageable) {
		return courseAdministration.listCourseApprovals(managerId, pageable);
	}

	@Transactional(readOnly = true)
	CourseApprovalResponse getCourseApproval(UUID managerId, UUID courseId) {
		return courseAdministration.getCourseApproval(managerId, courseId);
	}

	@Transactional
	CourseApprovalResponse approveCourse(UUID managerId, UUID courseId, CourseApprovalDecision request) {
		return courseAdministration.approveCourse(managerId, courseId, request);
	}

	@Transactional
	CourseApprovalResponse rejectCourse(UUID managerId, UUID courseId, CourseApprovalDecision request) {
		return courseAdministration.rejectCourse(managerId, courseId, request);
	}

	@Transactional
	CourseResponse patchCourse(UUID managerId, UUID courseId, CoursePatchRequest request) {
		return courseAdministration.patchCourse(managerId, courseId, request);
	}

	@Transactional
	public CourseResponse publishCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return courseAdministration.publishCourse(managerId, courseId, request);
	}

	@Transactional
	public CourseResponse deactivateCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return courseAdministration.deactivateCourse(managerId, courseId, request);
	}

	@Transactional
	public CourseResponse archiveCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return courseAdministration.archiveCourse(managerId, courseId, request);
	}

	@Transactional(readOnly = true)
	CourseRunPageResponse listRuns(UUID managerId, Pageable pageable) {
		return courseRunAdministration.listRuns(managerId, pageable);
	}

	@Transactional(readOnly = true)
	public CourseRunResponse getRun(UUID managerId, UUID runId) {
		return courseRunAdministration.getRun(managerId, runId);
	}

	@Transactional
	public CourseRunResponse createRun(UUID managerId, CourseRunCreateRequest request) {
		return courseRunAdministration.createRun(managerId, request);
	}

	@Transactional
	CourseRunResponse patchRun(UUID managerId, UUID runId, CourseRunPatchRequest request) {
		return courseRunAdministration.patchRun(managerId, runId, request);
	}

	@Transactional
	public CourseRunResponse openRun(UUID managerId, UUID runId, CourseRunOpenRequest request) {
		return courseRunAdministration.openRun(managerId, runId, request);
	}

	@Transactional
	public CourseRunResponse patchRunSchedule(UUID managerId, UUID runId, CourseRunScheduleRequest request) {
		return courseRunAdministration.patchRunSchedule(managerId, runId, request);
	}

	@Transactional
	public CourseRunResponse cancelRun(UUID managerId, UUID runId, VersionRequest request) {
		return courseRunAdministration.cancelRun(managerId, runId, request);
	}

	@Transactional(readOnly = true)
	Map<String, Object> cancellationImpact(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		if (!runs.existsById(runId))
			throw CourseException.runNotFound();
		return courseManagementRepository.cancellationImpactFindCourseEnrollments(runId, runId);
	}

	@Transactional(readOnly = true)
	public List<CourseSessionResponse> listSessions(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		if (!runs.existsById(runId))
			throw CourseException.runNotFound();
		List<CourseSession> values = sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId);
		Map<UUID, UUID> originalIds = originalSessionIds(values);
		return values.stream().map(value -> CourseSessionResponse.from(value, originalIds.get(value.getId()))).toList();
	}

	@Transactional(readOnly = true)
	CourseSessionResponse getSession(UUID managerId, UUID runId, UUID sessionId) {
		ensureActiveManager(managerId);
		CourseSession session = sessions.findByIdAndCourseRunId(sessionId, runId).orElseThrow(CourseException::sessionNotFound);
		return CourseSessionResponse.from(session, originalSessionId(session));
	}

	@Transactional
	public CourseSessionResponse createSession(UUID managerId, UUID runId, CourseSessionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null)
			throw CourseException.validation("request", "must be a JSON object");
		CourseSession session = sessionService.create(runId, request.unitNumber(), request.coachId(), request.roomId(), request.startAt(), request.endAt(), request.allocationVnd());
		audit(managerId, "COURSE_SESSION_CREATED", "COURSE_SESSION", session.getId(), null, snapshot(session));
		return CourseSessionResponse.from(session);
	}

	@Transactional
	public CourseSessionResponse patchSession(UUID managerId, UUID runId, UUID sessionId, CourseSessionPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		CourseSession current = sessions.findByIdAndCourseRunId(sessionId, runId).orElseThrow(CourseException::sessionNotFound);
		Map<String, Object> before = snapshot(current);
		CourseSession changed = sessionService.reschedule(sessionId, request.roomId(), request.coachId(), request.startAt(), request.endAt(), request.expectedVersion());
		audit(managerId, "COURSE_SESSION_RESCHEDULED", "COURSE_SESSION", sessionId, before, snapshot(changed));
		return CourseSessionResponse.from(changed, originalSessionId(changed));
	}

	private UUID originalSessionId(CourseSession session) {
		return session.isMakeup() ? sessions.findOriginalIdByReplacementId(session.getId()).orElse(null) : null;
	}

	private Map<UUID, UUID> originalSessionIds(List<CourseSession> values) {
		List<UUID> makeupIds = values.stream().filter(CourseSession::isMakeup).map(CourseSession::getId).toList();
		if (makeupIds.isEmpty())
			return Map.of();
		Map<UUID, UUID> result = new LinkedHashMap<>();
		for (CourseSessionRepository.OriginalSessionLink link : sessions.findOriginalIdsByReplacementIds(makeupIds)) result.put(link.getReplacementId(), link.getOriginalId());
		return result;
	}

	private void ensureActiveManager(UUID managerId) {
		courseAdministration.ensureActiveManager(managerId);
	}

	private String required(String value, int max, String field) {
		return courseAdministration.required(value, max, field);
	}

	private void audit(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> before, Map<String, Object> after) {
		courseAdministration.audit(actorId, action, targetType, targetId, before, after);
	}

	private Map<String, Object> snapshot(Course course) {
		return CourseManagementSnapshots.snapshot(course);
	}

	private Map<String, Object> snapshot(CourseRun run) {
		return CourseManagementSnapshots.snapshot(run);
	}

	private Map<String, Object> snapshot(CourseSession session) {
		return CourseManagementSnapshots.snapshot(session);
	}

	private final CourseAdministration courseAdministration;

	private final CourseRunAdministration courseRunAdministration;

	private final CourseManagementRepository courseManagementRepository;
}
