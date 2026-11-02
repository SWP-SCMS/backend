package com.scms.backend.course;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomRepository;
import com.scms.backend.scheduling.SharedResourceLockService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
class CourseRunRescheduleService {
	private final JdbcTemplate db;
	private final CourseRunRepository runs;
	private final CourseSessionRepository sessions;
	private final AccountRepository accounts;
	private final RoomRepository rooms;
	private final SharedResourceLockService locks;
	private final CourseSessionService sessionService;
	private final CourseScheduleChangeService scheduleChanges;
	private final AuditEventRepository audits;
	private final ObjectMapper json;
	private final Clock clock;

	CourseRunRescheduleService(JdbcTemplate db, CourseRunRepository runs, CourseSessionRepository sessions,
			AccountRepository accounts, RoomRepository rooms, SharedResourceLockService locks,
			CourseSessionService sessionService, CourseScheduleChangeService scheduleChanges,
			AuditEventRepository audits, ObjectMapper json, Clock clock) {
		this.db=db; this.runs=runs; this.sessions=sessions; this.accounts=accounts; this.rooms=rooms;
		this.locks=locks; this.sessionService=sessionService; this.scheduleChanges=scheduleChanges;
		this.audits=audits; this.json=json; this.clock=clock;
	}

	@Transactional
	CourseRunRescheduleResponse reschedule(UUID actorId, UUID runId, String key, CourseRunRescheduleRequest request) {
		if (!accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		Map<UUID, CourseSessionRescheduleRequest> requested = validate(key, request);
		String fingerprint = fingerprint(request);
		// Read only scalar resource IDs before locking, so managed schedules cannot remain stale.
		List<Map<String,Object>> resources = resources(runId);
		List<UUID> coachIds = new ArrayList<>(), roomIds = new ArrayList<>();
		for (var resource : resources) {
			coachIds.add((UUID) resource.get("coach_account_id")); roomIds.add((UUID) resource.get("room_id"));
		}
		for (var change : requested.values()) {
			if (change.coachId()!=null) coachIds.add(change.coachId());
			if (change.roomId()!=null) roomIds.add(change.roomId());
		}
		locks.lock(coachIds, roomIds);
		if (db.queryForList("select id from course_runs where id=? for update",UUID.class,runId).isEmpty()) throw CourseException.runNotFound();
		CourseRun run = runs.findById(runId).orElseThrow(CourseException::runNotFound);
		var saved = db.queryForList("select request_fingerprint,response_json::text from course_run_reschedules "
			+ "where course_run_id=? and actor_account_id=? and idempotency_key=?", runId, actorId, key);
		if (!saved.isEmpty()) {
			if (!fingerprint.equals(saved.getFirst().get("request_fingerprint"))) throw CourseException.rescheduleIdempotencyConflict();
			return json.readValue((String) saved.getFirst().get("response_json"), CourseRunRescheduleResponse.class);
		}
		if (!request.expectedVersion().equals(run.getVersion()) || !resources.equals(resources(runId)))
			throw CourseException.runChanged();
		if (Set.of(CourseRunStatus.COMPLETED, CourseRunStatus.CANCELLED, CourseRunStatus.SETTLEMENT_PENDING).contains(run.getStatus()))
			throw CourseException.invalidTransition();
		if (request.plannedStartAt().isBefore(run.getRegistrationClosesAt()))
			throw CourseException.validation("plannedStartAt", "must not precede registration closing");
		if (request.totalUnits()>run.getTotalUnits() && run.getStatus()!=CourseRunStatus.DRAFT)
			throw CourseException.validation("totalUnits", "cannot increase after the Run is opened");
		db.queryForList("select id from course_sessions where course_run_id=? order by id for update", UUID.class, runId);
		List<CourseSession> current = sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId);
		if (!current.stream().map(CourseSession::getId).toList().containsAll(requested.keySet())) throw CourseException.sessionNotFound();
		Instant now = clock.instant();
		Set<UUID> removed = new HashSet<>();
		for (CourseSession session : current) {
			if (!session.isMakeup() && session.getUnitNumber()>request.totalUnits()
					&& session.getStatus()!=CourseSessionStatus.REMOVED_BY_RUN_CHANGE) {
				CourseSession effective = session.getReplacementSession()==null ? session : session.getReplacementSession();
				if (!effective.getStartAt().isAfter(now) || effective.getStatus()==CourseSessionStatus.COMPLETED)
					throw CourseException.validation("totalUnits", "cannot remove units that have already started");
				removed.add(session.getId()); removed.add(effective.getId());
			}
		}
		List<PlannedSession> plan = new ArrayList<>();
		for (CourseSession session : current) {
			var change = requested.get(session.getId());
			if (change!=null && !change.expectedVersion().equals(session.getVersion())) throw CourseException.sessionChanged();
			if (change!=null && (removed.contains(session.getId()) || !active(session) || !session.getStartAt().isAfter(now)
					|| !change.startAt().isAfter(now))) throw CourseException.invalidSession();
			Instant start = change==null ? session.getStartAt() : change.startAt();
			Instant end = change==null ? session.getEndAt() : change.endAt();
			Account coach = change==null || change.coachId()==null ? session.getCoach()
				: accounts.findById(change.coachId()).orElseThrow(CourseException::resourceConflict);
			Room room = change==null || change.roomId()==null ? session.getRoom()
				: rooms.findById(change.roomId()).orElseThrow(CourseException::resourceConflict);
			if (!removed.contains(session.getId()) && session.getStatus()!=CourseSessionStatus.REMOVED_BY_RUN_CHANGE
					&& session.getReplacementSession()==null && (start.isBefore(request.plannedStartAt()) || end.isAfter(request.plannedEndAt())))
				throw CourseException.validation("plannedStartAt", "retained effective sessions must fit within the final Run window");
			plan.add(new PlannedSession(session, start, end, coach, room, removed.contains(session.getId())));
		}
		for (int i=0; i<plan.size(); i++) {
			PlannedSession item=plan.get(i);
			if (item.removed() || !active(item.session())) continue;
			sessionService.validateAssignment(run.getCapacity(), item.coach(), item.room(), item.startAt(), item.endAt());
			if (Boolean.TRUE.equals(db.queryForObject("""
				select exists(select 1 from course_sessions where course_run_id<>? and status in ('SCHEDULED','MAKEUP_SCHEDULED')
				 and (coach_account_id=? or room_id=?) and start_at<? and end_at>?)
				""", Boolean.class, runId, item.coach().getId(), item.room().getId(), timestamp(item.endAt()), timestamp(item.startAt()))))
				throw CourseException.resourceConflict();
			for (int j=0; j<i; j++) {
				PlannedSession other=plan.get(j);
				if (!other.removed() && active(other.session()) && overlaps(item,other)
						&& (item.coach().getId().equals(other.coach().getId()) || item.room().getId().equals(other.room().getId())))
					throw CourseException.resourceConflict();
			}
		}
		CourseWithdrawalCause cause = request.totalUnits()<run.getTotalUnits() ? CourseWithdrawalCause.REDUCED_UNIT_COUNT
			: Duration.between(run.getPlannedStartAt(),request.plannedStartAt()).compareTo(Duration.ofDays(7))>0
				? CourseWithdrawalCause.START_DELAY_OVER_7_DAYS : CourseWithdrawalCause.VOLUNTARY;
		boolean runChanged = !run.getPlannedStartAt().equals(request.plannedStartAt()) || !run.getPlannedEndAt().equals(request.plannedEndAt())
			|| run.getTotalUnits()!=request.totalUnits();
		var responses = scheduleChanges.prepareAtomicRunResponses(run.getId(), cause, runChanged, plan, now);
		UUID eventId = UUID.randomUUID();
		Map<String,Object> before = auditData(Map.of("run", CourseRunResponse.from(run), "sessions", current.stream().map(CourseSessionResponse::from).toList()));
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,actor_account_id,cause_code,old_planned_start_at,
			 old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units) values(?,?,?,?,?,?,?,?,?,?)
			""", eventId,runId,actorId,cause.name(),timestamp(run.getPlannedStartAt()),timestamp(run.getPlannedEndAt()),
			timestamp(request.plannedStartAt()),timestamp(request.plannedEndAt()),run.getTotalUnits(),request.totalUnits());
		run.changeSchedule(request.plannedStartAt(),request.plannedEndAt(),request.totalUnits());
		for (PlannedSession item : plan) {
			if (item.removed()) {
				item.session().removeByRunChange();
				db.update("update course_enrollment_sessions set disposition='REMOVED_BY_RUN_CHANGE' where course_session_id=? and disposition='ENROLLED'", item.session().getId());
			} else if (requested.containsKey(item.session().getId())) {
				item.session().reschedule(item.startAt(),item.endAt()); item.session().updateAssignment(item.room(),item.coach());
			}
		}
		runs.saveAndFlush(run); sessions.flush();
		scheduleChanges.writeAtomicRunResponses(eventId,run,responses,now);
		Map<UUID,UUID> originals = new LinkedHashMap<>();
		for (CourseSession s : current) if (s.getReplacementSession()!=null) originals.put(s.getReplacementSession().getId(),s.getId());
		CourseRunRescheduleResponse result = new CourseRunRescheduleResponse(CourseRunResponse.from(run), current.stream()
			.sorted(java.util.Comparator.comparing(CourseSession::getStartAt).thenComparing(s -> s.getId().toString()))
			.map(s -> CourseSessionResponse.from(s,originals.get(s.getId()))).toList(),eventId);
		audits.save(new AuditEvent(UUID.randomUUID(),actorId,"COURSE_RUN_RESCHEDULED","COURSE_RUN",runId,null,before,
			auditData(Map.of("run",result.run(),"sessions",result.sessions(),"changeEventId",eventId.toString(),"cause",cause.name()))));
		db.update("insert into course_run_reschedules(course_run_id,actor_account_id,idempotency_key,request_fingerprint,change_event_id,response_json) "
			+ "values(?,?,?,?,?,?::jsonb)",runId,actorId,key,fingerprint,eventId,json.writeValueAsString(result));
		return result;
	}

	private Map<UUID,CourseSessionRescheduleRequest> validate(String key, CourseRunRescheduleRequest request) {
		if (key==null || key.isBlank() || key.length()>100) throw CourseException.validation("Idempotency-Key","must contain 1 to 100 characters");
		if (request==null || request.expectedVersion()==null || request.expectedVersion()<0 || request.plannedStartAt()==null
				|| request.plannedEndAt()==null || !request.plannedStartAt().isBefore(request.plannedEndAt()) || request.totalUnits()==null
				|| request.totalUnits()<=0 || request.sessions()==null) throw CourseException.validation("schedule","must define a version, ordered window, positive units and sessions");
		Map<UUID,CourseSessionRescheduleRequest> result = new LinkedHashMap<>();
		for (var change : request.sessions()) {
			if (change==null || change.sessionId()==null || change.expectedVersion()==null || change.expectedVersion()<0
					|| change.startAt()==null || change.endAt()==null || !change.startAt().isBefore(change.endAt()))
				throw CourseException.validation("sessions","must define IDs, versions and ordered session times");
			if (result.put(change.sessionId(),change)!=null) throw CourseException.validation("sessions","session IDs must be unique");
		}
		return result;
	}

	private List<Map<String,Object>> resources(UUID runId) {
		return db.queryForList("select id,coach_account_id,room_id from course_sessions where course_run_id=? order by id",runId);
	}
	private String fingerprint(CourseRunRescheduleRequest request) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(request))); }
		catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
	}
	private Map<String,Object> auditData(Map<String,Object> value) {
		return json.convertValue(value,new tools.jackson.core.type.TypeReference<Map<String,Object>>() {});
	}
	static boolean active(CourseSession session) {
		return session.getStatus()==CourseSessionStatus.SCHEDULED || session.getStatus()==CourseSessionStatus.MAKEUP_SCHEDULED;
	}
	static boolean overlaps(PlannedSession a, PlannedSession b) { return a.startAt().isBefore(b.endAt()) && a.endAt().isAfter(b.startAt()); }
	private static Timestamp timestamp(Instant value) { return Timestamp.from(value); }
	record PlannedSession(CourseSession session, Instant startAt, Instant endAt, Account coach, Room room, boolean removed) {
		boolean changed() { return removed || !startAt.equals(session.getStartAt()) || !endAt.equals(session.getEndAt())
			|| !coach.getId().equals(session.getCoach().getId()) || !room.getId().equals(session.getRoom().getId()); }
	}
}
