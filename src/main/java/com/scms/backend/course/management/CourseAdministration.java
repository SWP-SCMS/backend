package com.scms.backend.course.management;

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
import com.scms.backend.course.CourseStatus;
import com.scms.backend.course.VersionRequest;
import com.scms.backend.course.run.CourseRunClosureService;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineStatus;
import com.scms.backend.scheduling.discipline.DisciplineRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

final class CourseAdministration {

	private final CourseRepository courses;

	private final CourseRunRepository runs;

	private final DisciplineRepository disciplines;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final CourseRunClosureService runClosure;

	private final Clock clock;

	CourseAdministration(CourseRepository courses, CourseRunRepository runs, DisciplineRepository disciplines, AccountRepository accounts, AuditEventRepository audits, CourseRunClosureService runClosure, Clock clock) {
		this.courses = courses;
		this.runs = runs;
		this.disciplines = disciplines;
		this.accounts = accounts;
		this.audits = audits;
		this.runClosure = runClosure;
		this.clock = clock;
	}

	CoursePageResponse listCourses(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CoursePageResponse.from(courses.findAll(pageable));
	}

	CourseResponse getCourse(UUID managerId, UUID courseId) {
		ensureActiveManager(managerId);
		return CourseResponse.from(courses.findDetailedById(courseId).orElseThrow(CourseException::notFound));
	}

	CourseResponse createCourse(UUID managerId, CourseCreateRequest request) {
		ensureActiveManager(managerId);
		return CourseResponse.from(createDraftCourse(managerId, request));
	}

	CourseApprovalPageResponse listCourseApprovals(UUID managerId, Pageable pageable) {
		ensureActiveManager(managerId);
		return CourseApprovalPageResponse.from(courses.findByStatus(CourseStatus.PENDING_APPROVAL, pageable));
	}

	CourseApprovalResponse getCourseApproval(UUID managerId, UUID courseId) {
		ensureActiveManager(managerId);
		return CourseApprovalResponse.from(courses.findSubmittedById(courseId).orElseThrow(CourseException::notFound));
	}

	CourseApprovalResponse approveCourse(UUID managerId, UUID courseId, CourseApprovalDecision request) {
		return decideCourse(managerId, courseId, request, true);
	}

	CourseApprovalResponse rejectCourse(UUID managerId, UUID courseId, CourseApprovalDecision request) {
		return decideCourse(managerId, courseId, request, false);
	}

	Course createDraftCourse(UUID actorId, CourseCreateRequest request) {
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

	CourseApprovalResponse decideCourse(UUID managerId, UUID courseId, CourseApprovalDecision request, boolean approved) {
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

	CourseResponse publishCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_PUBLISHED", Course::publish);
	}

	CourseResponse deactivateCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_DEACTIVATED", Course::deactivate);
	}

	CourseResponse archiveCourse(UUID managerId, UUID courseId, VersionRequest request) {
		return transitionCourse(managerId, courseId, request, "COURSE_ARCHIVED", Course::archive);
	}

	CourseResponse transitionCourse(UUID managerId, UUID courseId, VersionRequest request, String action, java.util.function.Consumer<Course> transition) {
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

	List<String> cancelUnstartedRuns(UUID managerId, UUID courseId, String reasonCode) {
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

	Course lockCourse(UUID id) {
		return courses.findByIdForUpdate(id).orElseThrow(CourseException::notFound);
	}

	CourseRun lockRun(UUID id) {
		return runs.findByIdForUpdate(id).orElseThrow(CourseException::runNotFound);
	}

	void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	String required(String value, int max, String field) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > max) {
			throw CourseException.validation(field, "must be non-blank and at most " + max + " characters");
		}
		return value.trim();
	}

	String optional(String value, int max, String field) {
		if (value == null)
			return null;
		if (value.length() > max) {
			throw CourseException.validation(field, "must be at most " + max + " characters");
		}
		return value.trim().isEmpty() ? null : value.trim();
	}

	void checkVersion(Long actual, Long expected, java.util.function.Supplier<CourseException> error) {
		if (!Objects.equals(actual, expected))
			throw error.get();
	}

	void audit(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> before, Map<String, Object> after) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, action, targetType, targetId, null, before, after));
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
}
