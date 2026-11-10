package com.scms.backend.course.management;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.Course;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseFieldLimits;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.CourseStatus;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.VersionRequest;
import com.scms.backend.course.refund.CourseUnitRefunds;
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
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineStatus;
import com.scms.backend.scheduling.discipline.DisciplineRepository;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
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
	}

	@Transactional(readOnly = true)
	CoursePageResponse listCourses(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CoursePageResponse.from(courses.findAll(pageable));
	}

	@Transactional(readOnly = true)
	public CourseResponse getCourse(UUID managerId, UUID courseId) {
		ensureActiveManager(managerId);
		return CourseResponse.from(courses.findDetailedById(courseId).orElseThrow(CourseException::notFound));
	}

	@Transactional
	public CourseResponse createCourse(UUID managerId, CourseCreateRequest request) {
		ensureActiveManager(managerId);
		return CourseResponse.from(createDraftCourse(managerId, request));
	}

	@Transactional(readOnly = true)
	CourseApprovalPageResponse listCourseApprovals(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CourseApprovalPageResponse.from(courses.findByStatus(CourseStatus.PENDING_APPROVAL, pageable));
	}

	@Transactional(readOnly = true)
	CourseApprovalResponse getCourseApproval(UUID managerId, UUID courseId) {
		ensureActiveManager(managerId);
		return CourseApprovalResponse.from(courses.findSubmittedById(courseId).orElseThrow(CourseException::notFound));
	}

	@Transactional
	CourseApprovalResponse approveCourse(UUID managerId, UUID courseId, CourseApprovalDecision request) {
		return decideCourse(managerId, courseId, request, true);
	}

	@Transactional
	CourseApprovalResponse rejectCourse(UUID managerId, UUID courseId, CourseApprovalDecision request) {
		return decideCourse(managerId, courseId, request, false);
	}

	private Course createDraftCourse(UUID actorId, CourseCreateRequest request) {
		if (request == null)
			throw CourseException.validation("request", "must be a JSON object");
		String code = required(request.code(), 50, "code").toUpperCase(java.util.Locale.ROOT);
		String name = required(request.name(), 200, "name");
		if (request.defaultMinimumEnrollment() != null && request.defaultMinimumEnrollment() <= 0) {
			throw CourseException.validation("defaultMinimumEnrollment", "must be greater than zero");
		}
		Discipline discipline = disciplines.findById(request.disciplineId()).orElseThrow(() -> CourseException.validation("disciplineId", "must identify an active discipline"));
		if (discipline.getStatus() != DisciplineStatus.ACTIVE) {
			throw CourseException.validation("disciplineId", "must identify an active discipline");
		}
		if (courses.existsByCode(code))
			throw CourseException.duplicateCode();
		String description = optional(request.description(), CourseFieldLimits.DESCRIPTION, "description");
		String imageUrl = optional(request.imageUrl(), CourseFieldLimits.IMAGE_URL, "imageUrl");
		Course course = new Course(UUID.randomUUID(), code, name, description, discipline, imageUrl, request.defaultMinimumEnrollment());
		try {
			courses.saveAndFlush(course);
		} catch (DataIntegrityViolationException exception) {
			throw CourseException.duplicateCode();
		}
		audit(actorId, "COURSE_CREATED", "COURSE", course.getId(), null, snapshot(course));
		return course;
	}

	private CourseApprovalResponse decideCourse(UUID managerId, UUID courseId, CourseApprovalDecision request, boolean approved) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		String reason = required(request.reason(), 1000, "reason");
		Course course = lockCourse(courseId);
		checkVersion(course.getVersion(), request.expectedVersion(), CourseException::changed);
		Map<String, Object> before = snapshot(course);
		if (approved)
			course.approve(managerId, reason);
		else
			course.reject(managerId, reason);
		courses.saveAndFlush(course);
		audit(managerId, approved ? "COURSE_APPROVED" : "COURSE_REJECTED", "COURSE", courseId, before, snapshot(course));
		return CourseApprovalResponse.from(course);
	}

	@Transactional
	CourseResponse patchCourse(UUID managerId, UUID courseId, CoursePatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		if (!request.hasMutableField()) {
			throw CourseException.validation("request", "at least one mutable field is required");
		}
		Course course = lockCourse(courseId);
		checkVersion(course.getVersion(), request.expectedVersion(), CourseException::changed);
		String name = request.hasName() ? required(request.name(), 200, "name") : course.getName();
		String description = request.hasDescription() ? optional(request.description(), CourseFieldLimits.DESCRIPTION, "description") : course.getDescription();
		String imageUrl = request.hasImageUrl() ? optional(request.imageUrl(), CourseFieldLimits.IMAGE_URL, "imageUrl") : course.getImageUrl();
		Integer minimum = request.hasDefaultMinimumEnrollment() ? request.defaultMinimumEnrollment() : course.getDefaultMinimumEnrollment();
		if (minimum != null && minimum <= 0) {
			throw CourseException.validation("defaultMinimumEnrollment", "must be greater than zero");
		}
		boolean changed = !Objects.equals(name, course.getName()) || !Objects.equals(description, course.getDescription()) || !Objects.equals(imageUrl, course.getImageUrl()) || !Objects.equals(minimum, course.getDefaultMinimumEnrollment());
		if (changed) {
			Map<String, Object> before = snapshot(course);
			course.update(name, description, imageUrl, minimum);
			Map<String, Object> after = snapshot(course);
			courses.saveAndFlush(course);
			audit(managerId, "COURSE_UPDATED", "COURSE", courseId, before, after);
		}
		return CourseResponse.from(course);
	}

	@Transactional
	public CourseResponse publishCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_PUBLISHED", Course::publish);
	}

	@Transactional
	public CourseResponse deactivateCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_DEACTIVATED", Course::deactivate);
	}

	@Transactional
	public CourseResponse archiveCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_ARCHIVED", Course::archive);
	}

	@Transactional(readOnly = true)
	CourseRunPageResponse listRuns(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CourseRunPageResponse.from(runs.findAll(pageable));
	}

	@Transactional(readOnly = true)
	public CourseRunResponse getRun(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		return CourseRunResponse.from(runs.findDetailedById(runId).orElseThrow(CourseException::runNotFound));
	}

	@Transactional
	public CourseRunResponse createRun(UUID managerId, CourseRunCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null)
			throw CourseException.validation("request", "must be a JSON object");
		String code = required(request.runCode(), 50, "runCode").toUpperCase(java.util.Locale.ROOT);
		validateRun(request);
		// Serialize creation with course deactivation so a new run cannot escape the cascade.
		Course course = lockCourse(request.courseId());
		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw CourseException.validation("courseId", "must identify a published course");
		}
		Account coach = accounts.findById(request.primaryCoachId()).orElseThrow(() -> CourseException.validation("primaryCoachId", "must identify an active coach"));
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE) {
			throw CourseException.validation("primaryCoachId", "must identify an active coach");
		}
		if (runs.existsByRunCode(code))
			throw CourseException.duplicateRunCode();
		CourseRun run = new CourseRun(UUID.randomUUID(), code, course, coach, request.tuitionVnd(), request.capacity(), request.minimumEnrollment(), request.registrationOpensAt(), request.registrationClosesAt(), request.plannedStartAt(), request.plannedEndAt(), request.totalUnits(), request.namedCoachCommitment());
		try {
			runs.saveAndFlush(run);
		} catch (DataIntegrityViolationException exception) {
			throw CourseException.duplicateRunCode();
		}
		audit(managerId, "COURSE_RUN_CREATED", "COURSE_RUN", run.getId(), null, snapshot(run));
		return CourseRunResponse.from(run);
	}

	@Transactional
	CourseRunResponse patchRun(UUID managerId, UUID runId, CourseRunPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		CourseRun run = lockRun(runId);
		checkVersion(run.getVersion(), request.expectedVersion(), CourseException::runChanged);
		Map<String, Object> before = snapshot(run);
		run.changeMinimumEnrollment(request.minimumEnrollment() == null ? 0 : request.minimumEnrollment());
		runs.saveAndFlush(run);
		audit(managerId, "COURSE_RUN_UPDATED", "COURSE_RUN", runId, before, snapshot(run));
		return CourseRunResponse.from(run);
	}

	@Transactional
	public CourseRunResponse openRun(UUID managerId, UUID runId, CourseRunOpenRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		CourseRun run = lockRun(runId);
		checkVersion(run.getVersion(), request.expectedVersion(), CourseException::runChanged);
		Map<String, Object> before = snapshot(run);
		validateOpeningSchedule(run);
		run.openForRegistration(request.policyVersion());
		runs.saveAndFlush(run);
		audit(managerId, "COURSE_RUN_OPENED", "COURSE_RUN", runId, before, snapshot(run));
		return CourseRunResponse.from(run);
	}

	private void validateOpeningSchedule(CourseRun run) {
		if (run.getCourse().getStatus() != CourseStatus.PUBLISHED)
			throw CourseException.validation("courseId", "Khóa học phải đang công khai trước khi mở đăng ký.");
		var schedule = sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId());
		if (schedule.size() != run.getTotalUnits())
			throw CourseException.validation("sessions", "Phải xếp đủ lịch cho tất cả buổi học trước khi mở đăng ký.");
		var numbers = new java.util.HashSet<Integer>();
		Instant previousEnd = null;
		Instant now = clock.instant();
		for (var session : schedule) {
			if (session.isMakeup() || session.getReplacementSession() != null || session.getStatus() != CourseSessionStatus.SCHEDULED || session.getUnitNumber() < 1 || session.getUnitNumber() > run.getTotalUnits() || !numbers.add(session.getUnitNumber()) || !session.getStartAt().isBefore(session.getEndAt()) || !session.getStartAt().isAfter(now) || session.getStartAt().isBefore(run.getPlannedStartAt()) || session.getEndAt().isAfter(run.getPlannedEndAt()) || (previousEnd != null && session.getStartAt().isBefore(previousEnd))) {
				throw CourseException.validation("sessions", "Lịch học phải đủ số buổi, không trùng giờ và nằm trong thời gian dự kiến.");
			}
			previousEnd = session.getEndAt();
		}
	}

	@Transactional
	public CourseRunResponse patchRunSchedule(UUID managerId, UUID runId, CourseRunScheduleRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null || request.plannedStartAt() == null || request.plannedEndAt() == null || request.totalUnits() == null || !request.plannedStartAt().isBefore(request.plannedEndAt()) || request.totalUnits() <= 0) {
			throw CourseException.validation("schedule", "must define an ordered schedule and positive totalUnits");
		}
		CourseRun run = lockRun(runId);
		checkVersion(run.getVersion(), request.expectedVersion(), CourseException::runChanged);
		if (run.getStatus() == CourseRunStatus.COMPLETED || run.getStatus() == CourseRunStatus.CANCELLED || run.getStatus() == CourseRunStatus.SETTLEMENT_PENDING)
			throw CourseException.invalidTransition();
		if (request.plannedStartAt().isBefore(run.getRegistrationClosesAt()))
			throw CourseException.validation("plannedStartAt", "must not precede registration closing");
		if (request.totalUnits() > run.getTotalUnits() && run.getStatus() != CourseRunStatus.DRAFT)
			throw CourseException.validation("totalUnits", "cannot increase after the Run is opened");
		List<CourseSession> current = sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId);
		for (CourseSession session : current) {
			if (session.getStatus() == CourseSessionStatus.REMOVED_BY_RUN_CHANGE)
				continue;
			if (!session.isMakeup() && session.getUnitNumber() > request.totalUnits()) {
				CourseSession effective = session.getReplacementSession() == null ? session : session.getReplacementSession();
				if (!effective.getStartAt().isAfter(clock.instant()) || effective.getStatus() == CourseSessionStatus.COMPLETED)
					throw CourseException.validation("totalUnits", "cannot remove units that have already started");
			}
			if (session.getUnitNumber() <= request.totalUnits() && session.getReplacementSession() == null && (session.getStartAt().isBefore(request.plannedStartAt()) || session.getEndAt().isAfter(request.plannedEndAt()))) {
				throw CourseException.validation("plannedStartAt", "retained sessions must have a schedule inside the Run window");
			}
		}
		CourseWithdrawalCause cause = scheduleCause(run, request);
		Map<String, Object> before = snapshot(run);
		Instant oldStart = run.getPlannedStartAt(), oldEnd = run.getPlannedEndAt();
		int oldUnits = run.getTotalUnits();
		run.changeSchedule(request.plannedStartAt(), request.plannedEndAt(), request.totalUnits());
		runs.saveAndFlush(run);
		UUID eventId = UUID.randomUUID();
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,actor_account_id,cause_code,
			old_planned_start_at,old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,?,?,?,?,?,?,?)
			""", eventId, runId, managerId, cause.name(), Timestamp.from(oldStart), Timestamp.from(oldEnd), Timestamp.from(request.plannedStartAt()), Timestamp.from(request.plannedEndAt()), oldUnits, request.totalUnits());
		if (request.totalUnits() < oldUnits) {
			for (CourseSession session : current) if (!session.isMakeup() && session.getUnitNumber() > request.totalUnits()) {
				session.removeByRunChange();
				sessions.saveAndFlush(session);
				if (session.getReplacementSession() != null) {
					session.getReplacementSession().removeByRunChange();
					sessions.saveAndFlush(session.getReplacementSession());
				}
				db.update("update course_enrollment_sessions set disposition='REMOVED_BY_RUN_CHANGE' " + "where course_run_id=? and unit_number=? and disposition='ENROLLED'", runId, session.getUnitNumber());
			}
		}
		sessions.flush();
		if (request.totalUnits() < oldUnits)
			CourseUnitRefunds.reduced(db, runId, eventId, managerId, clock.instant());
		if (scheduleChanges != null)
			scheduleChanges.createRunScheduleResponses(eventId, run, cause);
		Map<String, Object> after = snapshot(run);
		after.put("changeEventId", eventId.toString());
		after.put("cause", cause.name());
		audit(managerId, "COURSE_RUN_SCHEDULE_CHANGED", "COURSE_RUN", runId, before, after);
		return CourseRunResponse.from(run);
	}

	private CourseWithdrawalCause scheduleCause(CourseRun run, CourseRunScheduleRequest request) {
		if (request.totalUnits() < run.getTotalUnits())
			return CourseWithdrawalCause.REDUCED_UNIT_COUNT;
		if (Duration.between(run.getPlannedStartAt(), request.plannedStartAt()).compareTo(Duration.ofDays(7)) > 0)
			return CourseWithdrawalCause.START_DELAY_OVER_7_DAYS;
		return CourseWithdrawalCause.VOLUNTARY;
	}

	@Transactional
	public CourseRunResponse cancelRun(UUID managerId, UUID runId, VersionRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		if (request.reason() != null && (request.reason().trim().length() < 5 || request.reason().length() > 1000))
			throw CourseException.validation("reason", "must contain between 5 and 1000 characters");
		CourseRun run = lockRun(runId);
		checkVersion(run.getVersion(), request.expectedVersion(), CourseException::runChanged);
		Map<String, Object> before = snapshot(run);
		run.cancel();
		runs.saveAndFlush(run);
		runClosure.cancelEffects(managerId, run);
		Map<String, Object> after = snapshot(run);
		after.put("reasonCode", "MANAGER_CANCELLED");
		if (request.reason() != null)
			after.put("reason", request.reason().trim());
		audit(managerId, "COURSE_RUN_CANCELLED", "COURSE_RUN", runId, before, after);
		return CourseRunResponse.from(run);
	}

	@Transactional(readOnly = true)
	Map<String, Object> cancellationImpact(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		if (!runs.existsById(runId))
			throw CourseException.runNotFound();
		return db.queryForMap("""
			select (select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')) as "enrollmentCount",
			       (select count(*) from course_orders where course_run_id=? and status='PENDING_PAYMENT') as "pendingOrderCount"
			""", runId, runId);
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

	private CourseResponse transitionCourse(UUID managerId, UUID courseId, VersionRequest request, String action, java.util.function.Consumer<Course> transition) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		Course course = lockCourse(courseId);
		checkVersion(course.getVersion(), request.expectedVersion(), CourseException::changed);
		Map<String, Object> before = snapshot(course);
		transition.accept(course);
		courses.saveAndFlush(course);
		Map<String, Object> after = snapshot(course);
		if (course.getStatus() == CourseStatus.INACTIVE || course.getStatus() == CourseStatus.ARCHIVED) {
			after.put("cancelledRunIds", cancelUnstartedRuns(managerId, courseId, action));
		}
		audit(managerId, action, "COURSE", courseId, before, after);
		return CourseResponse.from(course);
	}

	private List<String> cancelUnstartedRuns(UUID managerId, UUID courseId, String reasonCode) {
		var cancelledIds = new java.util.ArrayList<String>();
		for (UUID runId : runs.findIdsByCourseId(courseId)) {
			CourseRun run = lockRun(runId);
			// Check after locking, including the wall-clock boundary if the scheduler is late.
			Instant now = clock.instant();
			if (!run.getPlannedStartAt().isAfter(now) || (run.getStatus() != CourseRunStatus.DRAFT && run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION && run.getStatus() != CourseRunStatus.CONFIRMED))
				continue;
			Map<String, Object> beforeRun = snapshot(run);
			run.cancel();
			runs.saveAndFlush(run);
			runClosure.cancelEffects(managerId, run, now);
			Map<String, Object> afterRun = snapshot(run);
			afterRun.put("reasonCode", reasonCode);
			afterRun.put("courseId", courseId.toString());
			audit(managerId, "COURSE_RUN_CANCELLED", "COURSE_RUN", runId, beforeRun, afterRun);
			cancelledIds.add(runId.toString());
		}
		return cancelledIds;
	}

	private Course lockCourse(UUID id) {
		return courses.findByIdForUpdate(id).orElseThrow(CourseException::notFound);
	}

	private CourseRun lockRun(UUID id) {
		return runs.findByIdForUpdate(id).orElseThrow(CourseException::runNotFound);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void validateRun(CourseRunCreateRequest request) {
		BigInteger tuition = request.tuitionVnd();
		if (tuition == null || tuition.signum() <= 0 || tuition.toString().length() > 19) {
			throw CourseException.validation("tuitionVnd", "must be a positive integer with at most 19 digits");
		}
		if (request.capacity() <= 0)
			throw CourseException.validation("capacity", "must be greater than zero");
		if (request.minimumEnrollment() <= 0 || request.minimumEnrollment() > request.capacity()) {
			throw CourseException.validation("minimumEnrollment", "must be between 1 and capacity");
		}
		if (request.registrationOpensAt() == null || request.registrationClosesAt() == null || request.plannedStartAt() == null || request.plannedEndAt() == null || !request.registrationOpensAt().isBefore(request.registrationClosesAt()) || request.registrationClosesAt().isAfter(request.plannedStartAt()) || !request.plannedStartAt().isBefore(request.plannedEndAt())) {
			throw CourseException.validation("schedule", "must define ordered half-open registration and course windows");
		}
		if (request.totalUnits() <= 0)
			throw CourseException.validation("totalUnits", "must be greater than zero");
	}

	private String required(String value, int max, String field) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > max) {
			throw CourseException.validation(field, "must be non-blank and at most " + max + " characters");
		}
		return value.trim();
	}

	private String optional(String value, int max, String field) {
		if (value == null)
			return null;
		if (value.length() > max) {
			throw CourseException.validation(field, "must be at most " + max + " characters");
		}
		return value.trim().isEmpty() ? null : value.trim();
	}

	private void checkVersion(Long actual, Long expected, java.util.function.Supplier<CourseException> error) {
		if (!Objects.equals(actual, expected))
			throw error.get();
	}

	private void audit(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> before, Map<String, Object> after) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, action, targetType, targetId, null, before, after));
	}

	private Map<String, Object> snapshot(Course course) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("code", course.getCode());
		data.put("name", course.getName());
		data.put("description", course.getDescription());
		data.put("disciplineId", course.getDiscipline().getId());
		data.put("imageUrl", course.getImageUrl());
		data.put("defaultMinimumEnrollment", course.getDefaultMinimumEnrollment());
		data.put("status", course.getStatus().name());
		data.put("submittedByAccountId", course.getSubmittedByAccountId());
		data.put("submittedAt", course.getSubmittedAt() == null ? null : course.getSubmittedAt().toString());
		data.put("reviewedByAccountId", course.getReviewedByAccountId());
		data.put("reviewedAt", course.getReviewedAt() == null ? null : course.getReviewedAt().toString());
		data.put("reviewReason", course.getReviewReason());
		return data;
	}

	private Map<String, Object> snapshot(CourseRun run) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("runCode", run.getRunCode());
		data.put("courseId", run.getCourse().getId());
		data.put("primaryCoachId", run.getPrimaryCoach().getId());
		data.put("tuitionVnd", run.getTuitionVnd());
		data.put("capacity", run.getCapacity());
		data.put("minimumEnrollment", run.getMinimumEnrollment());
		data.put("registrationOpensAt", run.getRegistrationOpensAt().toString());
		data.put("registrationClosesAt", run.getRegistrationClosesAt().toString());
		data.put("plannedStartAt", run.getPlannedStartAt().toString());
		data.put("plannedEndAt", run.getPlannedEndAt().toString());
		data.put("totalUnits", run.getTotalUnits());
		data.put("namedCoachCommitment", run.isNamedCoachCommitment());
		data.put("policyVersionSnapshot", run.getPolicyVersionSnapshot());
		data.put("status", run.getStatus().name());
		return data;
	}

	private Map<String, Object> snapshot(CourseSession session) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("runId", session.getCourseRun().getId());
		data.put("unitNumber", session.getUnitNumber());
		data.put("roomId", session.getRoom().getId());
		data.put("coachId", session.getCoach().getId());
		data.put("startAt", session.getStartAt().toString());
		data.put("endAt", session.getEndAt().toString());
		data.put("allocationVnd", session.getAllocationVnd());
		data.put("status", session.getStatus().name());
		return data;
	}
}
