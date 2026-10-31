package com.scms.backend.course;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineRepository;
import com.scms.backend.scheduling.DisciplineStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseManagementService {

	private final CourseRepository courses;
	private final CourseRunRepository runs;
	private final CourseSessionRepository sessions;
	private final DisciplineRepository disciplines;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final CourseSessionService sessionService;
	private final CourseRunClosureService runClosure;

	CourseManagementService(CourseRepository courses, CourseRunRepository runs, CourseSessionRepository sessions,
			DisciplineRepository disciplines, AccountRepository accounts, AuditEventRepository audits,
			CourseSessionService sessionService, CourseRunClosureService runClosure) {
		this.courses = courses;
		this.runs = runs;
		this.sessions = sessions;
		this.disciplines = disciplines;
		this.accounts = accounts;
		this.audits = audits;
		this.sessionService = sessionService;
		this.runClosure = runClosure;
	}

	@Transactional(readOnly = true)
	CoursePageResponse listCourses(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CoursePageResponse.from(courses.findAll(pageable));
	}

	@Transactional(readOnly = true)
	CourseResponse getCourse(UUID managerId, UUID courseId) {
		ensureActiveManager(managerId);
		return CourseResponse.from(courses.findDetailedById(courseId).orElseThrow(CourseException::notFound));
	}

	@Transactional
	CourseResponse createCourse(UUID managerId, CourseCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw CourseException.validation("request", "must be a JSON object");
		String code = required(request.code(), 50, "code").toUpperCase(java.util.Locale.ROOT);
		String name = required(request.name(), 200, "name");
		if (request.defaultMinimumEnrollment() != null && request.defaultMinimumEnrollment() <= 0) {
			throw CourseException.validation("defaultMinimumEnrollment", "must be greater than zero");
		}
		Discipline discipline = disciplines.findById(request.disciplineId())
			.orElseThrow(() -> CourseException.validation("disciplineId", "must identify an active discipline"));
		if (discipline.getStatus() != DisciplineStatus.ACTIVE) {
			throw CourseException.validation("disciplineId", "must identify an active discipline");
		}
		if (courses.existsByCode(code)) throw CourseException.duplicateCode();
		Course course = new Course(UUID.randomUUID(), code, name,
			optional(request.description(), CourseFieldLimits.DESCRIPTION, "description"), discipline,
			optional(request.imageUrl(), CourseFieldLimits.IMAGE_URL, "imageUrl"), request.defaultMinimumEnrollment());
		try {
			courses.saveAndFlush(course);
		}
		catch (DataIntegrityViolationException exception) {
			throw CourseException.duplicateCode();
		}
		audit(managerId, "COURSE_CREATED", "COURSE", course.getId(), null, snapshot(course));
		return CourseResponse.from(course);
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
		String description = request.hasDescription()
			? optional(request.description(), CourseFieldLimits.DESCRIPTION, "description") : course.getDescription();
		String imageUrl = request.hasImageUrl()
			? optional(request.imageUrl(), CourseFieldLimits.IMAGE_URL, "imageUrl") : course.getImageUrl();
		Integer minimum = request.hasDefaultMinimumEnrollment()
			? request.defaultMinimumEnrollment() : course.getDefaultMinimumEnrollment();
		if (minimum != null && minimum <= 0) {
			throw CourseException.validation("defaultMinimumEnrollment", "must be greater than zero");
		}
		boolean changed = !Objects.equals(name, course.getName())
			|| !Objects.equals(description, course.getDescription())
			|| !Objects.equals(imageUrl, course.getImageUrl())
			|| !Objects.equals(minimum, course.getDefaultMinimumEnrollment());
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
	CourseResponse publishCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_PUBLISHED", Course::publish);
	}

	@Transactional
	CourseResponse deactivateCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_DEACTIVATED", Course::deactivate);
	}

	@Transactional
	CourseResponse archiveCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_ARCHIVED", Course::archive);
	}

	@Transactional(readOnly = true)
	CourseRunPageResponse listRuns(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CourseRunPageResponse.from(runs.findAll(pageable));
	}

	@Transactional(readOnly = true)
	CourseRunResponse getRun(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		return CourseRunResponse.from(runs.findDetailedById(runId).orElseThrow(CourseException::runNotFound));
	}

	@Transactional
	CourseRunResponse createRun(UUID managerId, CourseRunCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw CourseException.validation("request", "must be a JSON object");
		String code = required(request.runCode(), 50, "runCode").toUpperCase(java.util.Locale.ROOT);
		validateRun(request);
		Course course = courses.findDetailedById(request.courseId()).orElseThrow(CourseException::notFound);
		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw CourseException.validation("courseId", "must identify a published course");
		}
		Account coach = accounts.findById(request.primaryCoachId())
			.orElseThrow(() -> CourseException.validation("primaryCoachId", "must identify an active coach"));
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE) {
			throw CourseException.validation("primaryCoachId", "must identify an active coach");
		}
		if (runs.existsByRunCode(code)) throw CourseException.duplicateRunCode();
		CourseRun run = new CourseRun(UUID.randomUUID(), code, course, coach, request.tuitionVnd(),
			request.capacity(), request.minimumEnrollment(), request.registrationOpensAt(),
			request.registrationClosesAt(), request.plannedStartAt(), request.plannedEndAt(), request.totalUnits(),
			request.namedCoachCommitment());
		try {
			runs.saveAndFlush(run);
		}
		catch (DataIntegrityViolationException exception) {
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
	CourseRunResponse openRun(UUID managerId, UUID runId, CourseRunOpenRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		CourseRun run = lockRun(runId);
		checkVersion(run.getVersion(), request.expectedVersion(), CourseException::runChanged);
		Map<String, Object> before = snapshot(run);
		run.openForRegistration(request.policyVersion());
		runs.saveAndFlush(run);
		audit(managerId, "COURSE_RUN_OPENED", "COURSE_RUN", runId, before, snapshot(run));
		return CourseRunResponse.from(run);
	}

	@Transactional
	CourseRunResponse cancelRun(UUID managerId, UUID runId, VersionRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		CourseRun run = lockRun(runId);
		checkVersion(run.getVersion(), request.expectedVersion(), CourseException::runChanged);
		Map<String, Object> before = snapshot(run);
		run.cancel();
		runs.saveAndFlush(run);
		runClosure.cancelEffects(managerId, run);
		Map<String, Object> after = snapshot(run);
		after.put("reasonCode", "MANAGER_CANCELLED");
		audit(managerId, "COURSE_RUN_CANCELLED", "COURSE_RUN", runId, before, after);
		return CourseRunResponse.from(run);
	}

	@Transactional(readOnly = true)
	List<CourseSessionResponse> listSessions(UUID managerId, UUID runId) {
		ensureActiveManager(managerId);
		if (!runs.existsById(runId)) throw CourseException.runNotFound();
		return sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId).stream()
			.map(CourseSessionResponse::from).toList();
	}

	@Transactional(readOnly = true)
	CourseSessionResponse getSession(UUID managerId, UUID runId, UUID sessionId) {
		ensureActiveManager(managerId);
		return CourseSessionResponse.from(sessions.findByIdAndCourseRunId(sessionId, runId)
			.orElseThrow(CourseException::sessionNotFound));
	}

	@Transactional
	CourseSessionResponse createSession(UUID managerId, UUID runId, CourseSessionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw CourseException.validation("request", "must be a JSON object");
		CourseSession session = sessionService.create(runId, request.unitNumber(), request.coachId(), request.roomId(),
			request.startAt(), request.endAt(), request.allocationVnd());
		audit(managerId, "COURSE_SESSION_CREATED", "COURSE_SESSION", session.getId(), null, snapshot(session));
		return CourseSessionResponse.from(session);
	}

	@Transactional
	CourseSessionResponse patchSession(UUID managerId, UUID runId, UUID sessionId, CourseSessionPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		CourseSession current = sessions.findByIdAndCourseRunId(sessionId, runId)
			.orElseThrow(CourseException::sessionNotFound);
		Map<String, Object> before = snapshot(current);
		CourseSession changed = sessionService.reschedule(sessionId, request.roomId(), request.coachId(),
			request.startAt(), request.endAt(), request.expectedVersion());
		audit(managerId, "COURSE_SESSION_RESCHEDULED", "COURSE_SESSION", sessionId, before, snapshot(changed));
		return CourseSessionResponse.from(changed);
	}

	private CourseResponse transitionCourse(UUID managerId, UUID courseId, VersionRequest request, String action,
			java.util.function.Consumer<Course> transition) {
		ensureActiveManager(managerId);
		if (request == null || request.expectedVersion() == null) {
			throw CourseException.validation("expectedVersion", "is required");
		}
		Course course = lockCourse(courseId);
		checkVersion(course.getVersion(), request.expectedVersion(), CourseException::changed);
		Map<String, Object> before = snapshot(course);
		transition.accept(course);
		courses.saveAndFlush(course);
		audit(managerId, action, "COURSE", courseId, before, snapshot(course));
		return CourseResponse.from(course);
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
		if (request.capacity() <= 0) throw CourseException.validation("capacity", "must be greater than zero");
		if (request.minimumEnrollment() <= 0 || request.minimumEnrollment() > request.capacity()) {
			throw CourseException.validation("minimumEnrollment", "must be between 1 and capacity");
		}
		if (request.registrationOpensAt() == null || request.registrationClosesAt() == null
				|| request.plannedStartAt() == null || request.plannedEndAt() == null
				|| !request.registrationOpensAt().isBefore(request.registrationClosesAt())
				|| request.registrationClosesAt().isAfter(request.plannedStartAt())
				|| !request.plannedStartAt().isBefore(request.plannedEndAt())) {
			throw CourseException.validation("schedule", "must define ordered half-open registration and course windows");
		}
		if (request.totalUnits() <= 0) throw CourseException.validation("totalUnits", "must be greater than zero");
	}

	private String required(String value, int max, String field) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > max) {
			throw CourseException.validation(field, "must be non-blank and at most " + max + " characters");
		}
		return value.trim();
	}

	private String optional(String value, int max, String field) {
		if (value == null) return null;
		if (value.length() > max) {
			throw CourseException.validation(field, "must be at most " + max + " characters");
		}
		return value.trim().isEmpty() ? null : value.trim();
	}

	private void checkVersion(Long actual, Long expected, java.util.function.Supplier<CourseException> error) {
		if (!Objects.equals(actual, expected)) throw error.get();
	}

	private void audit(UUID actorId, String action, String targetType, UUID targetId,
			Map<String, Object> before, Map<String, Object> after) {
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
