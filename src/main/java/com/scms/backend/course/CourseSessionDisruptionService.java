package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
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
import com.scms.backend.notification.NotificationWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseSessionDisruptionService {
	private final JdbcTemplate db;
	private final CourseRunRepository runs;
	private final CourseSessionRepository sessions;
	private final CourseSessionService sessionService;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	CourseSessionDisruptionService(JdbcTemplate db, CourseRunRepository runs, CourseSessionRepository sessions,
			CourseSessionService sessionService, AccountRepository accounts, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.runs = runs;
		this.sessions = sessions;
		this.sessionService = sessionService;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
	}

	@Transactional
	CourseSessionResponse cancel(UUID managerId, UUID runId, UUID sessionId, Long expectedVersion) {
		ensureManager(managerId);
		if (expectedVersion == null || expectedVersion < 0) throw CourseException.sessionDisruptionConflict();
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::runNotFound);
		ensureDisruptible(run);
		CourseSession session = sessions.findByIdForUpdate(sessionId).filter(value -> value.getCourseRun().getId().equals(runId))
			.orElseThrow(CourseException::sessionNotFound);
		checkSessionVersion(session, expectedVersion);
		Instant now = clock.instant();
		if (!session.getStartAt().isAfter(now) || (session.getStatus() != CourseSessionStatus.SCHEDULED
				&& session.getStatus() != CourseSessionStatus.MAKEUP_SCHEDULED)) {
			throw CourseException.sessionDisruptionConflict();
		}
		CourseSession original = sessions.findOriginalIdByReplacementId(sessionId)
			.flatMap(sessions::findByIdForUpdate).orElse(null);
		if (original != null && original.getReplacementSession() == null) throw CourseException.sessionDisruptionConflict();
		List<UUID> recipients = enrolledMembers(sessionId);
		Map<String, Object> before = snapshot(session);
		session.cancelPendingMakeup();
		if (original != null) {
			original.linkReplacement(null, now);
			db.update("update course_enrollment_sessions set course_session_id=? where course_session_id=? and disposition='ENROLLED'",
				original.getId(), sessionId);
			sessions.saveAndFlush(original);
		}
		sessions.saveAndFlush(session);
		audit(managerId, "COURSE_SESSION_CANCELLED", sessionId, before, snapshot(session));
		notify(recipients, session, "COURSE_SESSION_CANCELLED", "cancelled");
		return CourseSessionResponse.from(session);
	}

	@Transactional
	CourseMakeupResponse scheduleMakeup(UUID managerId, UUID runId, UUID sessionId, CourseMakeupRequest request) {
		ensureManager(managerId);
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0
				|| request.roomId() == null || request.coachId() == null || request.startAt() == null
				|| request.endAt() == null || !request.startAt().isAfter(clock.instant())
				|| !request.startAt().isBefore(request.endAt())) {
			throw CourseException.sessionDisruptionConflict();
		}
		CourseSession draft = sessions.findByIdAndCourseRunId(sessionId, runId)
			.orElseThrow(CourseException::sessionNotFound);
		CourseSession makeup = sessionService.createMakeup(runId, draft.getUnitNumber(), request.coachId(),
			request.roomId(), request.startAt(), request.endAt(), draft.getAllocationVnd());
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::runNotFound);
		ensureDisruptible(run);
		CourseSession original = sessions.findByIdForUpdate(sessionId)
			.filter(value -> value.getCourseRun().getId().equals(runId)).orElseThrow(CourseException::sessionNotFound);
		checkSessionVersion(original, request.expectedVersion());
		if (original.getStatus() != CourseSessionStatus.CANCELLED_PENDING_MAKEUP
				|| original.getReplacementSession() != null || original.isMakeup()) {
			throw CourseException.sessionDisruptionConflict();
		}
		List<UUID> recipients = enrolledMembers(sessionId);
		Instant now = clock.instant();
		Map<String, Object> before = snapshot(original);
		original.linkReplacement(makeup, now);
		sessions.saveAndFlush(original);
		db.update("update course_enrollment_sessions set course_session_id=? where course_session_id=? and disposition='ENROLLED'",
			makeup.getId(), sessionId);
		audit(managerId, "COURSE_SESSION_MAKEUP_SCHEDULED", sessionId, before,
			Map.of("replacementSessionId", makeup.getId().toString(), "status", original.getStatus().name()));
		notify(recipients, makeup, "COURSE_SESSION_MAKEUP_SCHEDULED", "makeup scheduled");
		return new CourseMakeupResponse(CourseSessionResponse.from(original), CourseSessionResponse.from(makeup));
	}

	@Transactional
	List<UUID> advance(Instant now) {
		List<UUID> warnings = db.query("""
			update course_sessions set makeup_warning_at=?
			where status='CANCELLED_PENDING_MAKEUP' and replacement_session_id is null
			  and is_makeup=false and original_end_at<=? and makeup_warning_at is null
			returning id
			""", (row, index) -> row.getObject(1, UUID.class), Timestamp.from(now),
			Timestamp.from(now.minusSeconds(30L * 86400)));
		db.update("update course_runs set status='IN_PROGRESS',version=version+1,updated_at=? where status='CONFIRMED' and planned_start_at<=?",
			Timestamp.from(now), Timestamp.from(now));
		db.update("update course_sessions set status='COMPLETED',version=version+1,updated_at=? "
			+ "where status in ('SCHEDULED','MAKEUP_SCHEDULED') and end_at<=?",
			Timestamp.from(now), Timestamp.from(now));
		List<UUID> runIds = db.query("""
			select run.id from course_runs run
			where run.status='IN_PROGRESS'
			  and exists (select 1 from course_sessions s where s.course_run_id=run.id)
			  and not exists (select 1 from course_sessions s where s.course_run_id=run.id
			    and ((s.status in ('SCHEDULED','MAKEUP_SCHEDULED','CANCELLED_PENDING_MAKEUP') and s.end_at>?)))
			  and (select count(distinct s.unit_number) from course_sessions s
			    where s.course_run_id=run.id and s.is_makeup=false) = run.total_units
			order by run.id limit 100 for update of run skip locked
			""", (row, index) -> row.getObject(1, UUID.class), Timestamp.from(now));
		for (UUID runId : runIds) settle(runId, now);
		return warnings;
	}

	private void settle(UUID runId, Instant now) {
		db.update("update course_sessions set status='UNRESOLVED',version=version+1,updated_at=? "
			+ "where course_run_id=? and status='CANCELLED_PENDING_MAKEUP' and replacement_session_id is null "
			+ "and is_makeup=false",
			Timestamp.from(now), runId);
		db.update("update course_enrollment_sessions enrollment_session set disposition='UNRESOLVED' "
			+ "from course_sessions session where enrollment_session.course_session_id=session.id "
			+ "and session.course_run_id=? and session.status='UNRESOLVED' and enrollment_session.disposition='ENROLLED'",
			runId);
		db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd)
			select gen_random_uuid(),course_enrollment_id,course_run_id,'UNRESOLVED_SESSIONS',sum(allocation_vnd)
			from course_enrollment_sessions where course_run_id=? and disposition='UNRESOLVED'
			group by course_enrollment_id,course_run_id
			on conflict (course_enrollment_id,source_type) do nothing
			""", runId);
		int obligationCount = db.queryForObject("select count(*) from course_refund_obligations "
			+ "where course_run_id=? and source_type='UNRESOLVED_SESSIONS'", Integer.class, runId);
		db.update("update course_runs set status='SETTLEMENT_PENDING',version=version+1,updated_at=? "
			+ "where id=? and status='IN_PROGRESS'", Timestamp.from(now), runId);
		audits.save(new AuditEvent(UUID.randomUUID(), "COURSE_RUN_SETTLEMENT_PENDING", "COURSE_RUN", runId,
			Map.of("unresolvedRefundObligationCount", obligationCount)));
	}

	private List<UUID> enrolledMembers(UUID sessionId) {
		return db.query("select e.member_account_id from course_enrollment_sessions es "
			+ "join course_enrollments e on e.id=es.course_enrollment_id "
			+ "where es.course_session_id=? and es.disposition='ENROLLED' and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by e.member_account_id",
			(row, index) -> row.getObject(1, UUID.class), sessionId);
	}

	private void notify(List<UUID> recipients, CourseSession session, String type, String verb) {
		for (UUID memberId : recipients) {
			notifications.write("course-session:" + session.getId() + ":" + verb + ":" + memberId,
				memberId, type, "COURSE_SESSION", session.getId(), Map.of("courseRunId",
					session.getCourseRun().getId().toString(), "unitNumber", session.getUnitNumber()));
		}
	}

	private void audit(UUID actorId, String action, UUID sessionId, Map<String, Object> before,
			Map<String, Object> after) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, action, "COURSE_SESSION", sessionId,
			null, before, after));
	}

	private Map<String, Object> snapshot(CourseSession session) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", session.getStatus().name());
		result.put("unitNumber", session.getUnitNumber());
		result.put("startAt", session.getStartAt().toString());
		result.put("endAt", session.getEndAt().toString());
		result.put("replacementSessionId", session.getReplacementSession() == null ? null
			: session.getReplacementSession().getId().toString());
		return result;
	}

	private void checkSessionVersion(CourseSession session, Long expectedVersion) {
		if (!expectedVersion.equals(session.getVersion())) throw CourseException.sessionChanged();
	}

	private void ensureManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void ensureDisruptible(CourseRun run) {
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION
				&& run.getStatus() != CourseRunStatus.CONFIRMED && run.getStatus() != CourseRunStatus.IN_PROGRESS) {
			throw CourseException.sessionDisruptionConflict();
		}
	}

	record CourseMakeupResponse(CourseSessionResponse originalSession, CourseSessionResponse makeupSession) { }
}
