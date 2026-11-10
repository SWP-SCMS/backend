package com.scms.backend.course.management;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.Course;
import com.scms.backend.course.CourseException;
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
import com.scms.backend.course.session.CourseSessionRepository;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseRunAdministration {

	private final CourseRunRepository runs;

	private final CourseSessionRepository sessions;

	private final AccountRepository accounts;

	private final CourseRunClosureService runClosure;

	private final JdbcTemplate db;

	private final CourseScheduleChangeService scheduleChanges;

	private final Clock clock;

	private final CourseAdministration courseAdministration;

	CourseRunAdministration(CourseRunRepository runs, CourseSessionRepository sessions, AccountRepository accounts, CourseRunClosureService runClosure, JdbcTemplate db, CourseScheduleChangeService scheduleChanges, Clock clock, CourseAdministration courseAdministration) {
		this.runs = runs;
		this.sessions = sessions;
		this.accounts = accounts;
		this.runClosure = runClosure;
		this.db = db;
		this.scheduleChanges = scheduleChanges;
		this.clock = clock;
		this.courseAdministration = courseAdministration;
		this.courseManagementRepository = new CourseManagementRepository(this.db);
	}

	CourseRunPageResponse listRuns(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CourseRunPageResponse.from(runs.findAll(pageable));
	}

	CourseRunResponse getRun(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		return CourseRunResponse.from(runs.findDetailedById(runId).orElseThrow(CourseException::runNotFound));
	}

	CourseRunResponse createRun(UUID managerId, CourseRunCreateRequest request) {
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

	CourseRunResponse openRun(UUID managerId, UUID runId, CourseRunOpenRequest request) {
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

	void validateOpeningSchedule(CourseRun run) {
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

	CourseRunResponse patchRunSchedule(UUID managerId, UUID runId, CourseRunScheduleRequest request) {
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
		courseManagementRepository.patchRunScheduleInsertCourseScheduleChangeEvents(eventId, runId, managerId, cause.name(), Timestamp.from(oldStart), Timestamp.from(oldEnd), Timestamp.from(request.plannedStartAt()), Timestamp.from(request.plannedEndAt()), oldUnits, request.totalUnits());
		if (request.totalUnits() < oldUnits) {
			for (CourseSession session : current) if (!session.isMakeup() && session.getUnitNumber() > request.totalUnits()) {
				session.removeByRunChange();
				sessions.saveAndFlush(session);
				if (session.getReplacementSession() != null) {
					session.getReplacementSession().removeByRunChange();
					sessions.saveAndFlush(session.getReplacementSession());
				}
				courseManagementRepository.patchRunScheduleUpdateCourseEnrollmentSessions(runId, session.getUnitNumber());
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

	CourseWithdrawalCause scheduleCause(CourseRun run, CourseRunScheduleRequest request) {
		if (request.totalUnits() < run.getTotalUnits())
			return CourseWithdrawalCause.REDUCED_UNIT_COUNT;
		if (Duration.between(run.getPlannedStartAt(), request.plannedStartAt()).compareTo(Duration.ofDays(7)) > 0)
			return CourseWithdrawalCause.START_DELAY_OVER_7_DAYS;
		return CourseWithdrawalCause.VOLUNTARY;
	}

	CourseRunResponse cancelRun(UUID managerId, UUID runId, VersionRequest request) {
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

	Course lockCourse(UUID id) {
		return courseAdministration.lockCourse(id);
	}

	CourseRun lockRun(UUID id) {
		return courseAdministration.lockRun(id);
	}

	void ensureActiveManager(UUID managerId) {
		courseAdministration.ensureActiveManager(managerId);
	}

	void validateRun(CourseRunCreateRequest request) {
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

	String required(String value, int max, String field) {
		return courseAdministration.required(value, max, field);
	}

	void checkVersion(Long actual, Long expected, java.util.function.Supplier<CourseException> error) {
		courseAdministration.checkVersion(actual, expected, error);
	}

	void audit(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> before, Map<String, Object> after) {
		courseAdministration.audit(actorId, action, targetType, targetId, before, after);
	}

	Map<String, Object> snapshot(Course course) {
		return CourseManagementSnapshots.snapshot(course);
	}

	Map<String, Object> snapshot(CourseRun run) {
		return CourseManagementSnapshots.snapshot(run);
	}

	Map<String, Object> snapshot(CourseSession session) {
		return CourseManagementSnapshots.snapshot(session);
	}

	private final CourseManagementRepository courseManagementRepository;
}
