package com.scms.backend.course.session;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.refund.CourseUnitRefunds;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.notification.NotificationWriter;
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
public class CourseSessionDisruptionService {

	private final JdbcTemplate db;

	private final CourseRunRepository runs;

	private final CourseSessionRepository sessions;

	private final CourseSessionService sessionService;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final Clock clock;

	public CourseSessionDisruptionService(JdbcTemplate db, CourseRunRepository runs, CourseSessionRepository sessions, CourseSessionService sessionService, AccountRepository accounts, AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.runs = runs;
		this.sessions = sessions;
		this.sessionService = sessionService;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
		this.courseSessionDisruptionRepository = new CourseSessionDisruptionRepository(this.db);
	}

	@Transactional
	CourseSessionResponse cancel(UUID managerId, UUID runId, UUID sessionId, Long expectedVersion) {
		return cancelInternal(managerId, runId, sessionId, expectedVersion);
	}

	@Transactional
	public CourseSessionResponse cancelFromEndpoint(UUID managerId, UUID runId, UUID sessionId, Long expectedVersion) {
		return cancelInternal(managerId, runId, sessionId, expectedVersion);
	}

	private CourseSessionResponse cancelInternal(UUID managerId, UUID runId, UUID sessionId, Long expectedVersion) {
		ensureManager(managerId);
		if (expectedVersion == null || expectedVersion < 0)
			throw CourseException.sessionDisruptionConflict();
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::runNotFound);
		ensureDisruptible(run);
		CourseSession session = sessions.findByIdForUpdate(sessionId).filter(value -> value.getCourseRun().getId().equals(runId)).orElseThrow(CourseException::sessionNotFound);
		checkSessionVersion(session, expectedVersion);
		Instant now = clock.instant();
		if (!session.getStartAt().isAfter(now) || (session.getStatus() != CourseSessionStatus.SCHEDULED && session.getStatus() != CourseSessionStatus.MAKEUP_SCHEDULED)) {
			throw CourseException.sessionDisruptionConflict();
		}
		CourseSession original = sessions.findOriginalIdByReplacementId(sessionId).flatMap(sessions::findByIdForUpdate).orElse(null);
		if (original != null && original.getReplacementSession() == null)
			throw CourseException.sessionDisruptionConflict();
		List<UUID> recipients = enrolledMembers(sessionId);
		if (!recipients.isEmpty())
			throw CourseException.sessionHasMembers();
		Map<String, Object> before = snapshot(session);
		session.cancelPendingMakeup();
		if (original != null) {
			original.linkReplacement(null, now);
			courseSessionDisruptionRepository.cancelInternalUpdateCourseEnrollmentSessions(original.getId(), sessionId);
			sessions.saveAndFlush(original);
		}
		sessions.saveAndFlush(session);
		audit(managerId, "COURSE_SESSION_CANCELLED", sessionId, before, snapshot(session));
		notify(recipients, session, "COURSE_SESSION_CANCELLED", "cancelled");
		return CourseSessionResponse.from(session, original == null ? null : original.getId());
	}

	@Transactional
	public CourseMakeupResponse cancelWithMakeup(UUID managerId, UUID runId, UUID sessionId, CourseSessionCancelWithMakeupRequest request) {
		ensureManager(managerId);
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0 || request.roomId() == null || request.coachId() == null || request.startAt() == null || request.endAt() == null || !request.startAt().isAfter(clock.instant()) || !request.startAt().isBefore(request.endAt()))
			throw CourseException.sessionDisruptionConflict();
		CourseSession draft = sessions.findByIdAndCourseRunId(sessionId, runId).orElseThrow(CourseException::sessionNotFound);
		CourseSession makeup = sessionService.createMakeup(runId, draft.getUnitNumber(), request.coachId(), request.roomId(), request.startAt(), request.endAt(), draft.getAllocationVnd());
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::runNotFound);
		ensureDisruptible(run);
		CourseSession original = sessions.findByIdForUpdate(sessionId).filter(value -> value.getCourseRun().getId().equals(runId)).orElseThrow(CourseException::sessionNotFound);
		checkSessionVersion(original, request.expectedVersion());
		if (original.isMakeup() || !original.getStartAt().isAfter(clock.instant()) || original.getStatus() != CourseSessionStatus.SCHEDULED)
			throw CourseException.sessionDisruptionConflict();
		List<UUID> recipients = enrolledMembers(sessionId);
		if (!recipients.isEmpty())
			throw CourseException.sessionHasMembers();
		Map<String, Object> before = snapshot(original);
		Instant now = clock.instant();
		original.cancelPendingMakeup();
		original.linkReplacement(makeup, now);
		sessions.saveAndFlush(original);
		courseSessionDisruptionRepository.cancelWithMakeupUpdateCourseEnrollmentSessions(makeup.getId(), sessionId);
		sessionService.synchronizeRunWindow(run);
		audit(managerId, "COURSE_SESSION_CANCELLED", sessionId, before, snapshot(original));
		audit(managerId, "COURSE_SESSION_MAKEUP_SCHEDULED", sessionId, before, Map.of("replacementSessionId", makeup.getId().toString(), "status", original.getStatus().name()));
		notify(recipients, original, "COURSE_SESSION_CANCELLED", "cancelled");
		notify(recipients, makeup, "COURSE_SESSION_MAKEUP_SCHEDULED", "makeup scheduled");
		return new CourseMakeupResponse(CourseSessionResponse.from(original), CourseSessionResponse.from(makeup, original.getId()));
	}

	@Transactional
	public CourseMakeupResponse scheduleMakeup(UUID managerId, UUID runId, UUID sessionId, CourseMakeupRequest request) {
		ensureManager(managerId);
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0 || request.roomId() == null || request.coachId() == null || request.startAt() == null || request.endAt() == null || !request.startAt().isAfter(clock.instant()) || !request.startAt().isBefore(request.endAt())) {
			throw CourseException.sessionDisruptionConflict();
		}
		CourseSession draft = sessions.findByIdAndCourseRunId(sessionId, runId).orElseThrow(CourseException::sessionNotFound);
		CourseSession makeup = sessionService.createMakeup(runId, draft.getUnitNumber(), request.coachId(), request.roomId(), request.startAt(), request.endAt(), draft.getAllocationVnd());
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::runNotFound);
		ensureDisruptible(run);
		CourseSession original = sessions.findByIdForUpdate(sessionId).filter(value -> value.getCourseRun().getId().equals(runId)).orElseThrow(CourseException::sessionNotFound);
		checkSessionVersion(original, request.expectedVersion());
		if (original.getStatus() != CourseSessionStatus.CANCELLED_PENDING_MAKEUP || original.getReplacementSession() != null || original.isMakeup()) {
			throw CourseException.sessionDisruptionConflict();
		}
		List<UUID> recipients = enrolledMembers(sessionId);
		Instant now = clock.instant();
		Map<String, Object> before = snapshot(original);
		original.linkReplacement(makeup, now);
		sessions.saveAndFlush(original);
		courseSessionDisruptionRepository.scheduleMakeupUpdateCourseEnrollmentSessions(makeup.getId(), sessionId);
		sessionService.synchronizeRunWindow(run);
		audit(managerId, "COURSE_SESSION_MAKEUP_SCHEDULED", sessionId, before, Map.of("replacementSessionId", makeup.getId().toString(), "status", original.getStatus().name()));
		notify(recipients, makeup, "COURSE_SESSION_MAKEUP_SCHEDULED", "makeup scheduled");
		return new CourseMakeupResponse(CourseSessionResponse.from(original), CourseSessionResponse.from(makeup, original.getId()));
	}

	@Transactional
	public List<UUID> advance(Instant now) {
		Timestamp at = Timestamp.from(now), warningBefore = Timestamp.from(now.minusSeconds(30L * 86400));
		List<UUID> runIds = courseSessionDisruptionRepository.advanceFindCourseRuns(at, at, warningBefore, at, at);
		List<UUID> warnings = new ArrayList<>();
		for (UUID runId : runIds) {
			// Share Run-before-session lock order with attendance, withdrawal and schedule changes.
			courseSessionDisruptionRepository.advanceFindCourseSessions(runId);
			warnings.addAll(courseSessionDisruptionRepository.markMakeupWarnings(at, runId, warningBefore));
			courseSessionDisruptionRepository.advanceUpdateCourseRuns(at, runId, at);
			courseSessionDisruptionRepository.advanceUpdateCourseSessions(at, runId, at);
			if (Boolean.TRUE.equals(courseSessionDisruptionRepository.isRunCompletable(runId, at, at)))
				settle(runId, now);
		}
		return warnings;
	}

	private void settle(UUID runId, Instant now) {
		courseSessionDisruptionRepository.settleUpdateCourseSessions(Timestamp.from(now), runId);
		courseSessionDisruptionRepository.settleUpdateCourseEnrollmentSessions(runId);
		CourseUnitRefunds.settled(db, runId, now);
		int obligationCount = courseSessionDisruptionRepository.settleFindCourseRefundObligations(runId);
		if (courseSessionDisruptionRepository.settleUpdateCourseRuns(Timestamp.from(now), runId) == 1)
			audits.save(new AuditEvent(UUID.randomUUID(), "COURSE_RUN_SETTLEMENT_PENDING", "COURSE_RUN", runId, Map.of("unresolvedRefundObligationCount", obligationCount)));
		if (courseSessionDisruptionRepository.completeSettledRun(Timestamp.from(now), runId) != 1)
			return;
		audits.save(new AuditEvent(UUID.randomUUID(), "COURSE_RUN_COMPLETED", "COURSE_RUN", runId, Map.of("status", "COMPLETED", "unresolvedRefundObligationCount", obligationCount)));
		for (UUID memberId : courseSessionDisruptionRepository.settleFindCourseEnrollments(runId)) notifications.write("course-run-completed:" + runId + ":" + memberId, memberId, "COURSE_RUN_COMPLETED", "COURSE_RUN", runId, Map.of("courseRunId", runId.toString()));
	}

	private List<UUID> enrolledMembers(UUID sessionId) {
		return courseSessionDisruptionRepository.enrolledMembersFindCourseEnrollmentSessions(sessionId);
	}

	private void notify(List<UUID> recipients, CourseSession session, String type, String verb) {
		for (UUID memberId : recipients) {
			notifications.write("course-session:" + session.getId() + ":" + verb + ":" + memberId, memberId, type, "COURSE_SESSION", session.getId(), Map.of("courseRunId", session.getCourseRun().getId().toString(), "unitNumber", session.getUnitNumber()));
		}
	}

	private void audit(UUID actorId, String action, UUID sessionId, Map<String, Object> before, Map<String, Object> after) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, action, "COURSE_SESSION", sessionId, null, before, after));
	}

	private Map<String, Object> snapshot(CourseSession session) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", session.getStatus().name());
		result.put("unitNumber", session.getUnitNumber());
		result.put("startAt", session.getStartAt().toString());
		result.put("endAt", session.getEndAt().toString());
		result.put("replacementSessionId", session.getReplacementSession() == null ? null : session.getReplacementSession().getId().toString());
		return result;
	}

	private void checkSessionVersion(CourseSession session, Long expectedVersion) {
		if (!expectedVersion.equals(session.getVersion()))
			throw CourseException.sessionChanged();
	}

	private void ensureManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void ensureDisruptible(CourseRun run) {
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION && run.getStatus() != CourseRunStatus.CONFIRMED && run.getStatus() != CourseRunStatus.IN_PROGRESS) {
			throw CourseException.sessionDisruptionConflict();
		}
	}

	public record CourseMakeupResponse(CourseSessionResponse originalSession, CourseSessionResponse makeupSession) {
	}

	private final CourseSessionDisruptionRepository courseSessionDisruptionRepository;
}
