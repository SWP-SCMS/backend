package com.scms.backend.course;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseWaitlistService {
	private record Waiter(UUID id, UUID memberId) { }
	private record WaitlistEntry(UUID id, UUID runId, UUID memberId) { }

	private final JdbcTemplate db;
	private final CourseRunRepository runs;
	private final AccountRepository accounts;
	private final CourseOrderService orders;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	CourseWaitlistService(JdbcTemplate db, CourseRunRepository runs, AccountRepository accounts,
			CourseOrderService orders, AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.db = db; this.runs = runs; this.accounts = accounts; this.orders = orders;
		this.audits = audits; this.notifications = notifications; this.clock = clock;
	}

	static Instant invitationExpiresAt(Instant issuedAt, Instant registrationClosesAt, Instant nextSessionAt) {
		Instant expiry = issuedAt.plusSeconds(24 * 3600);
		if (registrationClosesAt.isBefore(expiry)) expiry = registrationClosesAt;
		if (nextSessionAt != null && nextSessionAt.isBefore(expiry)) expiry = nextSessionAt;
		return expiry;
	}

	@Transactional
	CourseWaitlistResponse join(UUID memberId, UUID runId) {
		lockActiveMember(memberId);
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseWaitlistException::unavailable);
		Instant now = clock.instant();
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || !now.isBefore(run.getRegistrationClosesAt()))
			throw CourseWaitlistException.unavailable();
		if (hasEnrollmentOrHold(memberId, runId, now)) throw CourseWaitlistException.duplicate();
		if (usedSeats(runId, now) < run.getCapacity()) throw CourseWaitlistException.capacityAvailable();
		UUID id = UUID.randomUUID();
		try {
			db.update("insert into course_waitlists(id,course_run_id,member_account_id,status,created_at,updated_at) values (?,?,?,'WAITING',?,?)",
				id, runId, memberId, Timestamp.from(now), Timestamp.from(now));
		}
		catch (DataIntegrityViolationException exception) { throw CourseWaitlistException.duplicate(); }
		audits.save(new AuditEvent(UUID.randomUUID(), memberId, "COURSE_WAITLIST_JOINED", "COURSE_WAITLIST", id,
			Map.of("courseRunId", runId.toString())));
		return find(id);
	}

	@Transactional
	CourseWaitlistResponse leave(UUID memberId, UUID runId) {
		lockActiveMember(memberId);
		runs.findByIdForUpdate(runId).orElseThrow(CourseWaitlistException::notFound);
		UUID id = activeId(memberId, runId);
		Instant now = clock.instant();
		db.update("update course_waitlists set status='LEFT',updated_at=? where id=? and status in ('WAITING','INVITED')",
			Timestamp.from(now), id);
		audits.save(new AuditEvent(UUID.randomUUID(), memberId, "COURSE_WAITLIST_LEFT", "COURSE_WAITLIST", id,
			Map.of("courseRunId", runId.toString())));
		return find(id);
	}

	@Transactional(readOnly = true)
	List<CourseWaitlistResponse> mine(UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		return db.query(selectWithPosition() + " where member_account_id=? order by created_at desc,id desc",
			this::map, memberId);
	}

	@Transactional(noRollbackFor = CourseWaitlistException.class)
	CourseOrderService.CreateResult accept(UUID memberId, UUID waitlistId, String idempotencyKey) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		UUID runId = db.query("select course_run_id from course_waitlists where id=? and member_account_id=?",
			(rs, row) -> rs.getObject(1, UUID.class), waitlistId, memberId).stream().findFirst()
			.orElseThrow(CourseWaitlistException::notFound);
		runs.findByIdForUpdate(runId).orElseThrow(CourseWaitlistException::notFound);
		lockActiveMember(memberId);
		Instant now = clock.instant();
		var row = db.queryForMap("select status,invitation_expires_at,accepted_order_id from course_waitlists where id=? for update", waitlistId);
		if ("ACCEPTED".equals(row.get("status"))) {
			return new CourseOrderService.CreateResult(
				orders.getForMember(memberId, (UUID) row.get("accepted_order_id")), false);
		}
		if (!"INVITED".equals(row.get("status"))) throw CourseWaitlistException.notFound();
		Instant expiry = ((Timestamp) row.get("invitation_expires_at")).toInstant();
		if (!now.isBefore(expiry)) {
			db.update("update course_waitlists set status='EXPIRED',updated_at=? where id=?", Timestamp.from(now), waitlistId);
			throw CourseWaitlistException.invitationExpired();
		}
		CourseOrderService.CreateResult result = orders.createFromWaitlist(memberId, runId, idempotencyKey);
		db.update("update course_waitlists set status='ACCEPTED',accepted_order_id=?,updated_at=? where id=?",
			result.response().orderId(), Timestamp.from(now), waitlistId);
		audits.save(new AuditEvent(UUID.randomUUID(), memberId, "COURSE_WAITLIST_ACCEPTED", "COURSE_WAITLIST",
			waitlistId, Map.of("courseRunId", runId.toString(), "orderId", result.response().orderId().toString())));
		return result;
	}

	@Transactional
	boolean promote(UUID runId, Instant now) {
		UUID candidateMemberId = db.queryForList("select member_account_id from course_waitlists where course_run_id=? and status='WAITING' order by created_at,id limit 1",
			UUID.class, runId).stream().findFirst().orElse(null);
		if (candidateMemberId == null) {
			CourseRun run = runs.findByIdForUpdate(runId).orElse(null);
			if (run == null) return false;
			return db.update("update course_waitlists set status='EXPIRED',updated_at=? where course_run_id=? and status='INVITED' and invitation_expires_at<=?",
				Timestamp.from(now), runId, Timestamp.from(now)) > 0;
		}
		CourseRun run = runs.findByIdForUpdate(runId).orElse(null);
		if (run == null) return false;
		var member = accounts.findByIdForUpdate(candidateMemberId).orElse(null);
		int expired = db.update("update course_waitlists set status='EXPIRED',updated_at=? where course_run_id=? and status='INVITED' and invitation_expires_at<=?",
			Timestamp.from(now), runId, Timestamp.from(now));
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || !now.isBefore(run.getRegistrationClosesAt())) {
			int cancelled = db.update("update course_waitlists set status='CANCELLED',updated_at=? where course_run_id=? and status in ('WAITING','INVITED')",
				Timestamp.from(now), runId);
			return expired + cancelled > 0;
		}
		if (member == null || member.getRole() != AccountRole.MEMBER || member.getStatus() != AccountStatus.ACTIVE) {
			int cancelled = db.update("update course_waitlists set status='CANCELLED',updated_at=? where course_run_id=? and member_account_id=? and status='WAITING'",
				Timestamp.from(now), runId, candidateMemberId);
			return expired + cancelled > 0;
		}
		if (usedSeats(runId, now) >= run.getCapacity()) return expired > 0;
		Waiter waiting = db.query("select id,member_account_id from course_waitlists where course_run_id=? and status='WAITING' order by created_at,id limit 1 for update skip locked",
			(rs, row) -> new Waiter(rs.getObject("id", UUID.class), rs.getObject("member_account_id", UUID.class)), runId)
			.stream().findFirst().orElse(null);
		if (waiting == null || !waiting.memberId().equals(candidateMemberId)) return expired > 0;
		Instant nextSession = db.queryForList("select start_at from course_sessions where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED') and start_at>? order by start_at,id limit 1",
			Timestamp.class, runId, Timestamp.from(now)).stream().map(Timestamp::toInstant).findFirst().orElse(null);
		Instant expiresAt = invitationExpiresAt(now, run.getRegistrationClosesAt(), nextSession);
		if (!expiresAt.isAfter(now)) return expired > 0;
		db.update("update course_waitlists set status='INVITED',invited_at=?,invitation_expires_at=?,updated_at=? where id=? and status='WAITING'",
			Timestamp.from(now), Timestamp.from(expiresAt), Timestamp.from(now), waiting.id());
		notifications.write("course-waitlist-invited:" + waiting.id(), candidateMemberId, "COURSE_WAITLIST_INVITED",
			"COURSE_WAITLIST", waiting.id(), Map.of("courseRunId", runId.toString(), "expiresAt", expiresAt.toString()));
		return true;
	}

	@Transactional
	public void cancelForRun(UUID actorId, UUID runId) {
		List<WaitlistEntry> entries = db.query("select id,member_account_id from course_waitlists where course_run_id=? and status in ('WAITING','INVITED') for update",
			(rs, row) -> new WaitlistEntry(rs.getObject("id", UUID.class), runId,
				rs.getObject("member_account_id", UUID.class)), runId);
		if (entries.isEmpty()) return;
		db.update("update course_waitlists set status='CANCELLED',updated_at=current_timestamp where course_run_id=? and status in ('WAITING','INVITED')",
			runId);
		for (WaitlistEntry entry : entries) {
			audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST",
				entry.id(), Map.of("courseRunId", runId.toString())));
			notifications.write("course-waitlist-cancelled:" + entry.id(), entry.memberId(),
				"COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST", entry.id(), Map.of("courseRunId", runId.toString()));
		}
	}

	@Transactional
	public int cancelForMember(UUID actorId, UUID memberId) {
		List<WaitlistEntry> entries = db.query("select id,course_run_id from course_waitlists where member_account_id=? and status in ('WAITING','INVITED') for update",
			(rs, row) -> new WaitlistEntry(rs.getObject("id", UUID.class),
				rs.getObject("course_run_id", UUID.class), memberId), memberId);
		if (entries.isEmpty()) return 0;
		db.update("update course_waitlists set status='CANCELLED',updated_at=current_timestamp where member_account_id=? and status in ('WAITING','INVITED')",
			memberId);
		for (WaitlistEntry entry : entries) {
			audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST",
				entry.id(), Map.of("courseRunId", entry.runId().toString(), "reason", "MEMBER_SUSPENDED")));
			notifications.write("course-waitlist-cancelled:" + entry.id(), memberId,
				"COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST", entry.id(),
				Map.of("courseRunId", entry.runId().toString(), "reason", "MEMBER_SUSPENDED"));
		}
		return entries.size();
	}

	List<UUID> candidateRunIds(Instant now, UUID afterRunId) {
		return db.queryForList("""
			select distinct course_run_id from course_waitlists
			where (status='WAITING' or (status='INVITED' and invitation_expires_at<=?))
			  and (cast(? as uuid) is null or course_run_id>?)
			order by course_run_id limit 100
			""", UUID.class, Timestamp.from(now), afterRunId, afterRunId);
	}

	private long usedSeats(UUID runId, Instant now) {
		return db.queryForObject("""
			select (select count(*) from course_reservations where course_run_id=?
				and (status='ENROLLED' or (status='HELD' and expires_at>?)))
			 + (select count(*) from course_waitlists where course_run_id=? and status='INVITED' and invitation_expires_at>?)
			""", Long.class, runId, Timestamp.from(now), runId, Timestamp.from(now));
	}

	private boolean hasEnrollmentOrHold(UUID memberId, UUID runId, Instant now) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists(select 1 from course_enrollments where member_account_id=? and course_run_id=?)
			 or exists(select 1 from course_reservations where member_account_id=? and course_run_id=?
				and (status='ENROLLED' or (status='HELD' and expires_at>?)))
			 or exists(select 1 from course_waitlists where member_account_id=? and course_run_id=? and status in ('WAITING','INVITED'))
			""", Boolean.class, memberId, runId, memberId, runId, Timestamp.from(now), memberId, runId));
	}

	private UUID activeId(UUID memberId, UUID runId) {
		return db.queryForList("select id from course_waitlists where member_account_id=? and course_run_id=? and status in ('WAITING','INVITED') for update",
			UUID.class, memberId, runId).stream().findFirst().orElseThrow(CourseWaitlistException::notFound);
	}

	private CourseWaitlistResponse find(UUID id) {
		return db.query(selectWithPosition() + " where id=?", this::map, id).stream().findFirst()
			.orElseThrow(CourseWaitlistException::notFound);
	}

	private String selectWithPosition() {
		return """
			select waitlist.*,
				case when waitlist.status='WAITING' then (
					select count(*)::integer+1 from course_waitlists earlier
					where earlier.course_run_id=waitlist.course_run_id and earlier.status='WAITING'
					  and (earlier.created_at,earlier.id)<(waitlist.created_at,waitlist.id)
				) end as queue_position
			from course_waitlists waitlist
			""";
	}

	private CourseWaitlistResponse map(ResultSet rs, int row) throws SQLException {
		Timestamp invited = rs.getTimestamp("invited_at");
		Timestamp expires = rs.getTimestamp("invitation_expires_at");
		Integer position = rs.getObject("queue_position", Integer.class);
		return new CourseWaitlistResponse(rs.getObject("id", UUID.class), rs.getObject("course_run_id", UUID.class),
			rs.getString("status"), position, invited == null ? null : invited.toInstant(),
			expires == null ? null : expires.toInstant(), rs.getTimestamp("created_at").toInstant());
	}

	private void lockActiveMember(UUID memberId) {
		var account = accounts.findByIdForUpdate(memberId).orElseThrow(InvalidAuthenticatedAccountException::new);
		if (account.getRole() != AccountRole.MEMBER || account.getStatus() != AccountStatus.ACTIVE)
			throw new InvalidAuthenticatedAccountException();
	}
}
