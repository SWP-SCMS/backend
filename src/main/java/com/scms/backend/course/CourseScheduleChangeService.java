package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.scms.backend.course.CourseScheduleChangeApiModels.ScheduleChange;

@Service
class CourseScheduleChangeService {
	private final JdbcTemplate db;
	private final NotificationWriter notifications;
	private final AccountRepository accounts;
	private final Clock clock;
	private final CourseWithdrawalService withdrawals;
	private final AuditEventRepository audits;
	@Autowired(required = false) private CourseTransferService transfers;

	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts, Clock clock) {
		this(db, notifications, accounts, clock, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts,
			Clock clock, CourseWithdrawalService withdrawals, AuditEventRepository audits) {
		this.db = db; this.notifications = notifications; this.accounts = accounts; this.clock = clock;
		this.withdrawals = withdrawals; this.audits = audits;
	}

	@Transactional
	void notifyReschedule(CourseSession session, Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd) {
		if (oldStart.equals(newStart) && oldEnd.equals(newEnd)) return;
		Instant sentAt = clock.instant();
		boolean material = CourseScheduleChangePolicy.isMaterial(oldStart, oldEnd, newStart, newEnd);
		Set<UUID> conflicts = conflictingEnrollmentSessions(session.getCourseRun().getId(), session.getId(), newStart, newEnd);
		Instant deadline = material || !conflicts.isEmpty() ? CourseScheduleChangePolicy.deadline(sentAt, newStart) : null;
		CourseWithdrawalCause cause = CourseWithdrawalCause.VOLUNTARY;
		UUID eventId = UUID.randomUUID();
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id, e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by es.id
			""", session.getId());
		if (deadline != null && !members.isEmpty() && !deadline.isAfter(sentAt)) throw CourseException.responseDeadline();
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,cause_code,old_planned_start_at,
			old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,?,?,?,?,?,?)
			""", eventId, session.getCourseRun().getId(), cause.name(), timestamp(session.getCourseRun().getPlannedStartAt()),
			timestamp(session.getCourseRun().getPlannedEndAt()), timestamp(session.getCourseRun().getPlannedStartAt()),
			timestamp(session.getCourseRun().getPlannedEndAt()), session.getCourseRun().getTotalUnits(),
			session.getCourseRun().getTotalUnits());
		db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'"
			+ " and enrollment_session_id in (select id from course_enrollment_sessions where course_session_id=?)", session.getId());
		for (Map<String, Object> member : members) {
			boolean conflicted = conflicts.contains(member.get("enrollment_session_id"));
			CourseWithdrawalCause memberCause = conflicted ? CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT : cause;
			UUID recordId = material || conflicted ? UUID.randomUUID() : null;
			if (recordId != null) db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
				values(?,?,?,?,?,?,?,?,?,?)
				""", recordId, member.get("enrollment_session_id"), Timestamp.from(oldStart), Timestamp.from(oldEnd),
				Timestamp.from(newStart), Timestamp.from(newEnd), Timestamp.from(sentAt), Timestamp.from(deadline), memberCause.name(), eventId);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("courseCode", session.getCourseRun().getCourse().getCode());
			payload.put("runCode", session.getCourseRun().getRunCode());
			payload.put("unitNumber", session.getUnitNumber());
			payload.put("oldStartAt", oldStart.toString());
			payload.put("oldEndAt", oldEnd.toString());
			payload.put("newStartAt", newStart.toString());
			payload.put("newEndAt", newEnd.toString());
			payload.put("responseDeadline", recordId == null ? null : deadline.toString());
			payload.put("recordId", recordId);
			notifications.write("COURSE_SESSION_RESCHEDULED:" + session.getId() + ":" + session.getVersion()
				+ ":" + member.get("enrollment_session_id"), (UUID) member.get("member_account_id"),
				"COURSE_SCHEDULE_CHANGED", "COURSE_SESSION", session.getId(), payload);
		}
	}

	@Transactional(readOnly = true)
	List<ScheduleChange> listForReceptionist(UUID actorId, UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		return list(memberId);
	}

	@Transactional(readOnly = true)
	List<ScheduleChange> list(UUID memberId) {
		activeMember(memberId);
		Timestamp now = Timestamp.from(clock.instant());
		return db.query(select() + " where e.member_account_id=? order by r.created_at desc,r.id",
			this::map, now, now, memberId);
	}

	@Transactional
	ScheduleChange respond(UUID memberId, UUID responseId, String choice) {
		return respond(memberId, memberId, responseId, choice);
	}

	@Transactional
	ScheduleChange respond(UUID actorId, UUID memberId, UUID responseId, String choice) {
		if (!actorId.equals(memberId)) accounts.findByIdForUpdate(actorId)
			.filter(account -> account.getRole() == AccountRole.RECEPTIONIST && account.getStatus() == AccountStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedAccountException::new);
		activeMember(memberId);
		if (choice == null || !List.of("CONTINUE", "REQUEST_WITHDRAWAL", "REQUEST_TRANSFER").contains(choice))
			throw CourseException.invalidResponse();
		if (!"CONTINUE".equals(choice)) {
			List<UUID> runIds = db.queryForList("select e.course_run_id from course_schedule_change_responses r "
				+ "join course_enrollment_sessions es on es.id=r.enrollment_session_id "
				+ "join course_enrollments e on e.id=es.course_enrollment_id where r.id=? and e.member_account_id=?",
				UUID.class, responseId, memberId);
			if (runIds.isEmpty()) throw CourseException.responseNotFound();
			accounts.findByIdForUpdate(memberId).filter(account -> account.getRole() == AccountRole.MEMBER
				&& account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
			db.queryForObject("select id from course_runs where id=? for update", UUID.class, runIds.getFirst());
		}
		Timestamp queryTime = Timestamp.from(clock.instant());
		List<ScheduleChange> rows = db.query(select() + " where r.id=? and e.member_account_id=? for update of r",
			this::map, queryTime, queryTime, responseId, memberId);
		if (rows.isEmpty()) throw CourseException.responseNotFound();
		ScheduleChange current = rows.getFirst();
		if (current.response() != null) {
			if (current.response().equals(choice)) {
				if ("REQUEST_WITHDRAWAL".equals(choice) && withdrawals != null) {
					withdrawals.createForScheduleChange(actorId, memberId, responseId);
					return reload(memberId, responseId);
				}
				if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
					transfers.requestFromScheduleChange(actorId, memberId, responseId,
						current.respondedAt() == null ? clock.instant() : current.respondedAt());
				return current;
			}
			throw CourseException.responseConflict();
		}
		Instant now = clock.instant();
		if (!current.status().equals("PENDING") || !now.isBefore(current.responseDeadline()))
			throw CourseException.responseClosed();
		if ("REQUEST_WITHDRAWAL".equals(choice) && !current.withdrawalEligible())
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED",
				"Withdrawal is not allowed for the current Enrollment or CourseRun state.");
		db.update("update course_schedule_change_responses set response=?,responded_at=?,status='RESPONDED' where id=?",
			choice, Timestamp.from(now), responseId);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_SCHEDULE_CHANGE_RESPONDED",
			"COURSE_SCHEDULE_CHANGE_RESPONSE", responseId, Map.of("memberAccountId", memberId.toString(), "response", choice)));

		if ("REQUEST_WITHDRAWAL".equals(choice) && withdrawals != null) {
			withdrawals.createForScheduleChange(actorId, memberId, responseId);
			return reload(memberId, responseId);
		}
		if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
			transfers.requestFromScheduleChange(actorId, memberId, responseId, now);
		return reload(memberId, responseId);
	}

	@Transactional
	void createRunScheduleResponses(UUID eventId, CourseRun run, CourseWithdrawalCause cause) {
		Instant sentAt = clock.instant();
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id,e.member_account_id,s.start_at,s.end_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join course_sessions s on s.id=es.course_session_id
			where e.course_run_id=? and es.disposition in ('ENROLLED','REMOVED_BY_RUN_CHANGE') and s.start_at>?
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""", run.getId(), Timestamp.from(sentAt));
		if (members.isEmpty()) return;
		Instant nextStart = members.stream().map(row -> ((Timestamp) row.get("start_at")).toInstant())
			.min(Instant::compareTo).orElseThrow();
		Instant deadline = CourseScheduleChangePolicy.deadline(sentAt, nextStart);
		if (!deadline.isAfter(sentAt)) throw CourseException.responseDeadline();
		Set<UUID> conflicts = conflictingEnrollmentSessions(run.getId(), null, null, null);
		db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'"
			+ " and enrollment_session_id in (select id from course_enrollment_sessions where course_run_id=?)", run.getId());
		for (Map<String, Object> member : members) {
			CourseWithdrawalCause memberCause = cause == CourseWithdrawalCause.VOLUNTARY
				&& conflicts.contains(member.get("enrollment_session_id")) ? CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT : cause;
			UUID responseId = UUID.randomUUID();
			db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
				values(?,?,?,?,?,?,?,?,?,?)
				""", responseId, member.get("enrollment_session_id"), member.get("start_at"), member.get("end_at"),
				member.get("start_at"), member.get("end_at"), Timestamp.from(sentAt), Timestamp.from(deadline), memberCause.name(), eventId);
			notifications.write("COURSE_RUN_SCHEDULE_CHANGED:" + eventId + ":" + member.get("enrollment_session_id"),
				(UUID) member.get("member_account_id"), "COURSE_SCHEDULE_CHANGED", "COURSE_RUN", run.getId(),
				Map.of("recordId", responseId, "cause", memberCause.name(), "responseDeadline", deadline.toString()));
		}
	}

	private ScheduleChange reload(UUID memberId, UUID responseId) {
		Timestamp now = Timestamp.from(clock.instant());
		return db.query(select() + " where r.id=? and e.member_account_id=?", this::map, now, now, responseId, memberId)
			.stream().findFirst().orElseThrow(CourseException::responseNotFound);
	}

	List<AtomicResponse> prepareAtomicRunResponses(UUID runId, CourseWithdrawalCause cause, boolean runChanged,
			List<CourseRunRescheduleService.PlannedSession> plan, Instant now) {
		Map<UUID,CourseRunRescheduleService.PlannedSession> byId = new LinkedHashMap<>();
		for (var item : plan) byId.put(item.session().getId(),item);
		List<Map<String,Object>> members = db.queryForList("""
			select es.id enrollment_session_id,es.course_session_id,e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where e.course_run_id=? and es.disposition='ENROLLED'
			 and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""",runId);
		Map<UUID,List<Map<String,Object>>> membersBySession = members.stream().collect(java.util.stream.Collectors.groupingBy(row -> (UUID)row.get("course_session_id")));
		Map<UUID,List<CourseRunRescheduleService.PlannedSession>> memberSessions = new LinkedHashMap<>();
		for (var member : members) memberSessions.computeIfAbsent((UUID)member.get("member_account_id"),ignored -> new ArrayList<>())
			.add(byId.get(member.get("course_session_id")));
		List<AtomicResponse> responses = new ArrayList<>();
		for (var item : plan) {
			if ((!runChanged && !item.changed()) || !item.startAt().isAfter(now)) continue;
			Set<UUID> conflicts = item.removed() ? Set.of()
				: conflictingEnrollmentSessions(runId,item.session().getId(),item.startAt(),item.endAt(),true);
			for (var member : membersBySession.getOrDefault(item.session().getId(),List.of())) {
				boolean conflict = conflicts.contains(member.get("enrollment_session_id"));
				if (!item.removed()) for (var other : memberSessions.get(member.get("member_account_id"))) {
					if (other!=item && !other.removed() && CourseRunRescheduleService.active(other.session())
							&& CourseRunRescheduleService.overlaps(item,other)) conflict=true;
				}
				CourseWithdrawalCause memberCause = cause==CourseWithdrawalCause.VOLUNTARY && conflict
					? CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT : cause;
				boolean responseRequired = runChanged || conflict || item.removed() || CourseScheduleChangePolicy.isMaterial(
					item.session().getStartAt(),item.session().getEndAt(),item.startAt(),item.endAt());
				responses.add(new AtomicResponse((UUID)member.get("enrollment_session_id"),(UUID)member.get("member_account_id"),
					item.session().getId(),item.session().getStartAt(),item.session().getEndAt(),item.startAt(),item.endAt(),
					memberCause,responseRequired ? UUID.randomUUID() : null,null));
			}
		}
		Instant nextStart = responses.stream().filter(r -> r.responseId()!=null).map(AtomicResponse::newStart)
			.min(Instant::compareTo).orElse(null);
		Instant deadline = nextStart==null ? null : CourseScheduleChangePolicy.deadline(now,nextStart);
		if (deadline!=null && !deadline.isAfter(now)) throw CourseException.responseDeadline();
		return responses.stream().map(r -> new AtomicResponse(r.enrollmentSessionId(),r.memberId(),r.sessionId(),r.oldStart(),r.oldEnd(),
			r.newStart(),r.newEnd(),r.cause(),r.responseId(),r.responseId()==null ? null : deadline)).toList();
	}

	void writeAtomicRunResponses(UUID eventId, CourseRun run, List<AtomicResponse> responses, Instant now) {
		Map<UUID,List<Map<String,Object>>> notices = new LinkedHashMap<>();
		for (var r : responses) {
			db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING' and enrollment_session_id=?",r.enrollmentSessionId());
			if (r.responseId()!=null) db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				 new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id) values(?,?,?,?,?,?,?,?,?,?)
				""",r.responseId(),r.enrollmentSessionId(),timestamp(r.oldStart()),timestamp(r.oldEnd()),timestamp(r.newStart()),timestamp(r.newEnd()),
				timestamp(now),timestamp(r.deadline()),r.cause().name(),eventId);
			Map<String,Object> change = new LinkedHashMap<>();
			change.put("courseSessionId",r.sessionId()); change.put("recordId",r.responseId());
			change.put("oldStartAt",r.oldStart().toString()); change.put("oldEndAt",r.oldEnd().toString());
			change.put("newStartAt",r.newStart().toString()); change.put("newEndAt",r.newEnd().toString());
			change.put("cause",r.cause().name()); change.put("responseDeadline",r.deadline()==null ? null : r.deadline().toString());
			notices.computeIfAbsent(r.memberId(),ignored -> new ArrayList<>()).add(change);
		}
		for (var notice : notices.entrySet()) notifications.write("COURSE_RUN_RESCHEDULED:"+eventId+":"+notice.getKey(),notice.getKey(),
			"COURSE_SCHEDULE_CHANGED","COURSE_RUN",run.getId(),Map.of("changeEventId",eventId,"courseCode",run.getCourse().getCode(),
				"runCode",run.getRunCode(),"changes",notice.getValue()));
	}

	record AtomicResponse(UUID enrollmentSessionId, UUID memberId, UUID sessionId, Instant oldStart, Instant oldEnd,
		Instant newStart, Instant newEnd, CourseWithdrawalCause cause, UUID responseId, Instant deadline) { }

	private String select() {
		return """
			select r.*,w.id withdrawal_request_id,w.refund_basis_vnd,w.refund_amount_vnd,
			 w.status withdrawal_status,w.snapshot_captured_at,w.withdrawal_reason_snapshot,
			 e.lifecycle_status,e.total_units_snapshot,e.amount_paid_vnd_snapshot,run.status course_run_status,
			 u.remaining_units,u.unused_allocation
			from course_schedule_change_responses r join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id join course_runs run on run.id=e.course_run_id
			left join course_withdrawal_requests w on w.schedule_change_response_id=r.id
			left join lateral (select count(*) filter(where s.start_at>?) remaining_units,
			 coalesce(sum(unit.allocation_vnd) filter(where s.start_at>?),0) unused_allocation
			 from course_enrollment_sessions unit join course_sessions s on s.id=unit.course_session_id
			 where unit.course_enrollment_id=e.id) u on true
			""";
	}

	private ScheduleChange map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
		CourseWithdrawalCause cause = CourseWithdrawalCause.valueOf(rs.getString("cause_code"));
		CourseWithdrawalPolicy.Decision decision = CourseWithdrawalPolicy.evaluate(cause, rs.getInt("remaining_units"),
			rs.getInt("total_units_snapshot"), rs.getBigDecimal("unused_allocation").toBigIntegerExact()
				.min(rs.getBigDecimal("amount_paid_vnd_snapshot").toBigIntegerExact()));
		String reason = decision.reason();
		boolean eligible = decision.eligible();
		if (!List.of("ACTIVE", "WITHDRAWAL_PENDING").contains(rs.getString("lifecycle_status"))) {
			eligible = false; reason = "ENROLLMENT_NOT_ACTIVE";
		}
		else if (!List.of("OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS").contains(rs.getString("course_run_status"))) {
			eligible = false; reason = "COURSE_RUN_NOT_WITHDRAWABLE";
		}
		else if (!"REQUEST_WITHDRAWAL".equals(rs.getString("response"))
				&& (!"PENDING".equals(rs.getString("status")) || !clock.instant().isBefore(rs.getTimestamp("response_deadline").toInstant()))) {
			eligible = false; reason = "SCHEDULE_CHANGE_RESPONSE_CLOSED";
		}
		UUID withdrawalId = rs.getObject("withdrawal_request_id", UUID.class);
		if (withdrawalId != null) {
			eligible = "PENDING".equals(rs.getString("withdrawal_status")) && rs.getTimestamp("snapshot_captured_at") != null;
			reason = eligible ? rs.getString("withdrawal_reason_snapshot") : "REQUEST_NOT_PENDING";
		}
		return new ScheduleChange(rs.getObject("id", UUID.class), rs.getObject("enrollment_session_id", UUID.class),
			rs.getTimestamp("old_start_at").toInstant(), rs.getTimestamp("old_end_at").toInstant(),
			rs.getTimestamp("new_start_at").toInstant(), rs.getTimestamp("new_end_at").toInstant(),
			rs.getTimestamp("notification_sent_at").toInstant(), rs.getTimestamp("response_deadline").toInstant(),
			rs.getString("status"), rs.getString("response"), rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant(),
			rs.getObject("change_event_id", UUID.class), cause, rs.getObject("withdrawal_request_id", UUID.class), eligible, reason,
			withdrawalId == null ? decision.refundBasisVnd() : rs.getBigDecimal("refund_basis_vnd") == null ? null : rs.getBigDecimal("refund_basis_vnd").toBigIntegerExact(),
			withdrawalId == null ? decision.refundAmountVnd() : rs.getBigDecimal("refund_amount_vnd") == null ? null : rs.getBigDecimal("refund_amount_vnd").toBigIntegerExact());
	}

	private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }

	private Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt) {
		return conflictingEnrollmentSessions(runId,sessionId,startAt,endAt,false);
	}

	private Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt, boolean excludeCurrentRun) {
		return new HashSet<>(db.queryForList("""
			select distinct current_es.id
			from course_enrollment_sessions current_es
			join course_enrollments current_enrollment on current_enrollment.id=current_es.course_enrollment_id
			join course_sessions current_session on current_session.id=current_es.course_session_id
			where current_enrollment.course_run_id=? and current_es.disposition='ENROLLED'
			and current_enrollment.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			and (cast(? as uuid) is null or current_es.course_session_id=?)
			and (exists(select 1
				from course_enrollment_sessions other_es
				join course_enrollments other_enrollment on other_enrollment.id=other_es.course_enrollment_id
				join course_sessions other_session on other_session.id=other_es.course_session_id
				where other_enrollment.member_account_id=current_enrollment.member_account_id
				and (?=false or other_enrollment.course_run_id<>current_enrollment.course_run_id)
				and other_es.course_session_id<>current_es.course_session_id and other_es.disposition='ENROLLED'
				and other_enrollment.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
				and other_session.status in ('SCHEDULED','MAKEUP_SCHEDULED')
				and other_session.start_at < coalesce(cast(? as timestamptz),current_session.end_at)
				and other_session.end_at > coalesce(cast(? as timestamptz),current_session.start_at))
			or exists(select 1 from bookings b join class_sessions gym on gym.id=b.class_session_id
				where b.member_account_id=current_enrollment.member_account_id and b.status='BOOKED'
				and gym.start_time < coalesce(cast(? as timestamptz),current_session.end_at)
				and gym.end_time > coalesce(cast(? as timestamptz),current_session.start_at)))
			""", UUID.class, runId, sessionId, sessionId, excludeCurrentRun, timestamp(endAt), timestamp(startAt), timestamp(endAt), timestamp(startAt)));
	}

	private void activeMember(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}
}
