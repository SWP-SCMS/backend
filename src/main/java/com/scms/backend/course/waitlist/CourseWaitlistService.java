package com.scms.backend.course.waitlist;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseWaitlistException;
import com.scms.backend.course.order.CourseOrderService;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.notification.NotificationWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseWaitlistService {

	record Waiter(UUID id, UUID memberId) {
	}

	record WaitlistEntry(UUID id, UUID runId, UUID memberId) {
	}

	private final JdbcTemplate db;

	private final CourseRunRepository runs;

	private final AccountRepository accounts;

	private final CourseOrderService orders;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final Clock clock;

	CourseWaitlistService(JdbcTemplate db, CourseRunRepository runs, AccountRepository accounts, CourseOrderService orders, AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.runs = runs;
		this.accounts = accounts;
		this.orders = orders;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
		this.courseWaitlistRepository = new CourseWaitlistRepository(this.db);
	}

	static Instant invitationExpiresAt(Instant issuedAt, Instant registrationClosesAt, Instant nextSessionAt) {
		Instant expiry = issuedAt.plusSeconds(24 * 3600);
		if (registrationClosesAt.isBefore(expiry))
			expiry = registrationClosesAt;
		if (nextSessionAt != null && nextSessionAt.isBefore(expiry))
			expiry = nextSessionAt;
		return expiry;
	}

	@Transactional
	public CourseWaitlistResponse join(UUID memberId, UUID runId) {
		return join(memberId, memberId, runId);
	}

	@Transactional
	public CourseWaitlistResponse join(UUID actorId, UUID memberId, UUID runId) {
		if (!actorId.equals(memberId))
			accounts.findByIdForUpdate(actorId).filter(account -> account.getRole() == AccountRole.RECEPTIONIST && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		lockActiveMember(memberId);
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseWaitlistException::unavailable);
		Instant now = clock.instant();
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || !now.isBefore(run.getRegistrationClosesAt()))
			throw CourseWaitlistException.unavailable();
		if (hasEnrollmentOrHold(memberId, runId, now))
			throw CourseWaitlistException.duplicate();
		if (usedSeats(runId, now) < run.getCapacity())
			throw CourseWaitlistException.capacityAvailable();
		UUID id = UUID.randomUUID();
		try {
			courseWaitlistRepository.joinInsertCourseWaitlists(id, runId, memberId, Timestamp.from(now), Timestamp.from(now));
		} catch (DataIntegrityViolationException exception) {
			throw CourseWaitlistException.duplicate();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_JOINED", "COURSE_WAITLIST", id, Map.of("courseRunId", runId.toString())));
		return find(id);
	}

	@Transactional
	public CourseWaitlistResponse leave(UUID memberId, UUID runId) {
		return leave(memberId, memberId, runId);
	}

	@Transactional
	public CourseWaitlistResponse leave(UUID actorId, UUID memberId, UUID runId) {
		if (!actorId.equals(memberId))
			accounts.findByIdForUpdate(actorId).filter(account -> account.getRole() == AccountRole.RECEPTIONIST && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		lockActiveMember(memberId);
		runs.findByIdForUpdate(runId).orElseThrow(CourseWaitlistException::notFound);
		UUID id = activeId(memberId, runId);
		Instant now = clock.instant();
		courseWaitlistRepository.leaveUpdateCourseWaitlists(Timestamp.from(now), id);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_LEFT", "COURSE_WAITLIST", id, Map.of("courseRunId", runId.toString())));
		return find(id);
	}

	@Transactional(readOnly = true)
	public List<CourseWaitlistResponse> mineForReceptionist(UUID actorId, UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		return mine(memberId);
	}

	@Transactional(readOnly = true)
	List<CourseWaitlistResponse> mine(UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		return courseWaitlistRepository.mineFindRows(memberId);
	}

	@Transactional(noRollbackFor = CourseWaitlistException.class)
	public CourseOrderService.CreateResult accept(UUID memberId, UUID waitlistId, String idempotencyKey) {
		return accept(memberId, memberId, waitlistId, idempotencyKey);
	}

	@Transactional(noRollbackFor = CourseWaitlistException.class)
	public CourseOrderService.CreateResult accept(UUID actorId, UUID memberId, UUID waitlistId, String idempotencyKey) {
		if (!actorId.equals(memberId))
			accounts.findByIdForUpdate(actorId).filter(account -> account.getRole() == AccountRole.RECEPTIONIST && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		lockActiveMember(memberId);
		UUID runId = courseWaitlistRepository.acceptFindCourseWaitlists(waitlistId, memberId).stream().findFirst().orElseThrow(CourseWaitlistException::notFound);
		runs.findByIdForUpdate(runId).orElseThrow(CourseWaitlistException::notFound);
		Instant now = clock.instant();
		var row = courseWaitlistRepository.lockInvitationForAcceptance(waitlistId);
		if ("ACCEPTED".equals(row.get("status"))) {
			return new CourseOrderService.CreateResult(orders.getForMember(memberId, (UUID) row.get("accepted_order_id")), false);
		}
		if (!"INVITED".equals(row.get("status")))
			throw CourseWaitlistException.notFound();
		Instant expiry = ((Timestamp) row.get("invitation_expires_at")).toInstant();
		if (!now.isBefore(expiry)) {
			courseWaitlistRepository.acceptUpdateCourseWaitlists(Timestamp.from(now), waitlistId);
			throw CourseWaitlistException.invitationExpired();
		}
		CourseOrderService.CreateResult result = orders.createFromWaitlist(actorId, memberId, runId, idempotencyKey);
		courseWaitlistRepository.markInvitationAccepted(result.response().orderId(), Timestamp.from(now), waitlistId);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_ACCEPTED", "COURSE_WAITLIST", waitlistId, Map.of("courseRunId", runId.toString(), "orderId", result.response().orderId().toString())));
		return result;
	}

	@Transactional
	public boolean promote(UUID runId, Instant now) {
		UUID candidateMemberId = courseWaitlistRepository.promoteFindCourseWaitlists(runId).stream().findFirst().orElse(null);
		if (candidateMemberId == null) {
			CourseRun run = runs.findByIdForUpdate(runId).orElse(null);
			if (run == null)
				return false;
			return courseWaitlistRepository.expireInvitationsBeforePromotion(Timestamp.from(now), runId, Timestamp.from(now)) > 0;
		}
		var member = accounts.findByIdForUpdate(candidateMemberId).orElse(null);
		CourseRun run = runs.findByIdForUpdate(runId).orElse(null);
		if (run == null)
			return false;
		int expired = courseWaitlistRepository.expireInvitationsDuringPromotion(Timestamp.from(now), runId, Timestamp.from(now));
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || !now.isBefore(run.getRegistrationClosesAt())) {
			int cancelled = courseWaitlistRepository.cancelWaitingAndInvitedForRun(Timestamp.from(now), runId);
			return expired + cancelled > 0;
		}
		if (member == null || member.getRole() != AccountRole.MEMBER || member.getStatus() != AccountStatus.ACTIVE) {
			int cancelled = courseWaitlistRepository.cancelWaitingCandidate(Timestamp.from(now), runId, candidateMemberId);
			return expired + cancelled > 0;
		}
		if (usedSeats(runId, now) >= run.getCapacity())
			return expired > 0;
		Waiter waiting = courseWaitlistRepository.lockNextWaitingCandidate(runId).stream().findFirst().orElse(null);
		if (waiting == null || !waiting.memberId().equals(candidateMemberId))
			return expired > 0;
		Instant nextSession = courseWaitlistRepository.promoteFindCourseSessions(runId, Timestamp.from(now)).stream().map(Timestamp::toInstant).findFirst().orElse(null);
		Instant expiresAt = invitationExpiresAt(now, run.getRegistrationClosesAt(), nextSession);
		if (!expiresAt.isAfter(now))
			return expired > 0;
		courseWaitlistRepository.inviteWaitingCandidate(Timestamp.from(now), Timestamp.from(expiresAt), Timestamp.from(now), waiting.id());
		notifications.write("course-waitlist-invited:" + waiting.id(), candidateMemberId, "COURSE_WAITLIST_INVITED", "COURSE_WAITLIST", waiting.id(), Map.of("courseRunId", runId.toString(), "expiresAt", expiresAt.toString()));
		return true;
	}

	@Transactional
	public void cancelForRun(UUID actorId, UUID runId) {
		List<WaitlistEntry> entries = courseWaitlistRepository.cancelForRunFindCourseWaitlists(runId, runId);
		if (entries.isEmpty())
			return;
		courseWaitlistRepository.cancelForRunUpdateCourseWaitlists(runId);
		for (WaitlistEntry entry : entries) {
			audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST", entry.id(), Map.of("courseRunId", runId.toString())));
			notifications.write("course-waitlist-cancelled:" + entry.id(), entry.memberId(), "COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST", entry.id(), Map.of("courseRunId", runId.toString()));
		}
	}

	@Transactional
	public int cancelForMember(UUID actorId, UUID memberId) {
		List<WaitlistEntry> entries = courseWaitlistRepository.cancelForMemberFindCourseWaitlists(memberId, memberId);
		if (entries.isEmpty())
			return 0;
		courseWaitlistRepository.cancelForMemberUpdateCourseWaitlists(memberId);
		for (WaitlistEntry entry : entries) {
			audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST", entry.id(), Map.of("courseRunId", entry.runId().toString(), "reason", "MEMBER_SUSPENDED")));
			notifications.write("course-waitlist-cancelled:" + entry.id(), memberId, "COURSE_WAITLIST_CANCELLED", "COURSE_WAITLIST", entry.id(), Map.of("courseRunId", entry.runId().toString(), "reason", "MEMBER_SUSPENDED"));
		}
		return entries.size();
	}

	List<UUID> candidateRunIds(Instant now, UUID afterRunId) {
		return courseWaitlistRepository.candidateRunIdsFindCourseWaitlists(Timestamp.from(now), afterRunId, afterRunId);
	}

	private long usedSeats(UUID runId, Instant now) {
		return courseWaitlistRepository.usedSeatsFindCourseEnrollments(runId, runId, Timestamp.from(now), runId, Timestamp.from(now), runId, Timestamp.from(now));
	}

	private boolean hasEnrollmentOrHold(UUID memberId, UUID runId, Instant now) {
		return Boolean.TRUE.equals(courseWaitlistRepository.hasEnrollmentOrHoldFindCourseEnrollments(memberId, runId, memberId, runId, Timestamp.from(now), memberId, runId, memberId, runId, Timestamp.from(now)));
	}

	private UUID activeId(UUID memberId, UUID runId) {
		return courseWaitlistRepository.activeIdFindCourseWaitlists(memberId, runId).stream().findFirst().orElseThrow(CourseWaitlistException::notFound);
	}

	private CourseWaitlistResponse find(UUID id) {
		return courseWaitlistRepository.findFindRows(id).stream().findFirst().orElseThrow(CourseWaitlistException::notFound);
	}

	private CourseWaitlistResponse map(ResultSet rs, int row) throws SQLException {
		Timestamp invited = rs.getTimestamp("invited_at");
		Timestamp expires = rs.getTimestamp("invitation_expires_at");
		Integer position = rs.getObject("queue_position", Integer.class);
		return new CourseWaitlistResponse(rs.getObject("id", UUID.class), rs.getObject("course_run_id", UUID.class), rs.getString("course_name"), rs.getString("course_code"), rs.getString("run_code"), rs.getString("status"), position, invited == null ? null : invited.toInstant(), expires == null ? null : expires.toInstant(), rs.getTimestamp("created_at").toInstant());
	}

	private void lockActiveMember(UUID memberId) {
		var account = accounts.findByIdForUpdate(memberId).orElseThrow(InvalidAuthenticatedAccountException::new);
		if (account.getRole() != AccountRole.MEMBER || account.getStatus() != AccountStatus.ACTIVE)
			throw new InvalidAuthenticatedAccountException();
	}

	private final CourseWaitlistRepository courseWaitlistRepository;
}
