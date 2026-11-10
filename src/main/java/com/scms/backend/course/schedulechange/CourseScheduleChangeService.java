package com.scms.backend.course.schedulechange;

import static com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.CourseWithdrawalException;
import com.scms.backend.course.run.CourseRunRescheduleService;
import com.scms.backend.course.transfer.CourseTransferService;
import com.scms.backend.course.withdrawal.CourseWithdrawalService;
import com.scms.backend.notification.NotificationWriter;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseScheduleChangeService {

	private final JdbcTemplate db;

	private final NotificationWriter notifications;

	private final AccountRepository accounts;

	private final Clock clock;

	private final CourseWithdrawalService withdrawals;

	private final AuditEventRepository audits;

	@Autowired(required = false)
	private CourseTransferService transfers;

	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts, Clock clock) {
		this(db, notifications, accounts, clock, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts, Clock clock, CourseWithdrawalService withdrawals, AuditEventRepository audits) {
		this.db = db;
		this.notifications = notifications;
		this.accounts = accounts;
		this.clock = clock;
		this.withdrawals = withdrawals;
		this.audits = audits;
		this.courseScheduleChangeRepository = new CourseScheduleChangeRepository(this.db, this.clock);
		this.courseScheduleChangeNotifications = new CourseScheduleChangeNotifications(this.db, this.notifications, this.clock, this.courseScheduleChangeRepository);
	}

	@Transactional
	public void notifyReschedule(CourseSession session, Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd) {
		courseScheduleChangeNotifications.notifyReschedule(session, oldStart, oldEnd, newStart, newEnd);
	}

	@Transactional(readOnly = true)
	public List<ScheduleChange> listForReceptionist(UUID actorId, UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
		return list(memberId);
	}

	@Transactional(readOnly = true)
	public List<ScheduleChange> list(UUID memberId) {
		activeMember(memberId);
		Timestamp now = Timestamp.from(clock.instant());
		return courseScheduleChangeRepository.listFindRows(now, now, memberId);
	}

	@Transactional
	public ScheduleChange respond(UUID memberId, UUID responseId, String choice) {
		return respond(memberId, memberId, responseId, choice);
	}

	@Transactional
	public ScheduleChange respond(UUID actorId, UUID memberId, UUID responseId, String choice) {
		if (!actorId.equals(memberId))
			accounts.findByIdForUpdate(actorId).filter(account -> account.getRole() == AccountRole.RECEPTIONIST && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		activeMember(memberId);
		if (choice == null || !List.of("CONTINUE", "REQUEST_WITHDRAWAL", "REQUEST_TRANSFER").contains(choice))
			throw CourseException.invalidResponse();
		if (!"CONTINUE".equals(choice)) {
			List<UUID> runIds = courseScheduleChangeRepository.respondFindCourseScheduleChangeResponses(responseId, memberId);
			if (runIds.isEmpty())
				throw CourseException.responseNotFound();
			accounts.findByIdForUpdate(memberId).filter(account -> account.getRole() == AccountRole.MEMBER && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
			courseScheduleChangeRepository.respondFindCourseRuns(runIds.getFirst());
		}
		Timestamp queryTime = Timestamp.from(clock.instant());
		List<ScheduleChange> rows = courseScheduleChangeRepository.respondFindOf(queryTime, queryTime, responseId, memberId);
		if (rows.isEmpty())
			throw CourseException.responseNotFound();
		ScheduleChange current = rows.getFirst();
		if (current.response() != null) {
			if (current.response().equals(choice)) {
				if ("REQUEST_WITHDRAWAL".equals(choice) && withdrawals != null) {
					withdrawals.createForScheduleChange(actorId, memberId, responseId);
					return reload(memberId, responseId);
				}
				if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
					transfers.requestFromScheduleChange(actorId, memberId, responseId, current.respondedAt() == null ? clock.instant() : current.respondedAt());
				return current;
			}
			throw CourseException.responseConflict();
		}
		Instant now = clock.instant();
		if (!current.status().equals("PENDING") || !now.isBefore(current.responseDeadline()))
			throw CourseException.responseClosed();
		if ("REQUEST_WITHDRAWAL".equals(choice) && !current.withdrawalEligible())
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED", "Withdrawal is not allowed for the current Enrollment or CourseRun state.");
		courseScheduleChangeRepository.respondUpdateCourseScheduleChangeResponses(choice, Timestamp.from(now), responseId);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_SCHEDULE_CHANGE_RESPONDED", "COURSE_SCHEDULE_CHANGE_RESPONSE", responseId, Map.of("memberAccountId", memberId.toString(), "response", choice)));
		if ("REQUEST_WITHDRAWAL".equals(choice) && withdrawals != null) {
			withdrawals.createForScheduleChange(actorId, memberId, responseId);
			return reload(memberId, responseId);
		}
		if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
			transfers.requestFromScheduleChange(actorId, memberId, responseId, now);
		return reload(memberId, responseId);
	}

	@Transactional
	public void createRunScheduleResponses(UUID eventId, CourseRun run, CourseWithdrawalCause cause) {
		courseScheduleChangeNotifications.createRunScheduleResponses(eventId, run, cause);
	}

	private ScheduleChange reload(UUID memberId, UUID responseId) {
		return courseScheduleChangeRepository.reload(memberId, responseId);
	}

	public List<AtomicResponse> prepareAtomicRunResponses(UUID runId, CourseWithdrawalCause cause, boolean runChanged, List<CourseRunRescheduleService.PlannedSession> plan, Instant now) {
		return courseScheduleChangeNotifications.prepareAtomicRunResponses(runId, cause, runChanged, plan, now);
	}

	public void writeAtomicRunResponses(UUID eventId, CourseRun run, List<AtomicResponse> responses, Instant now) {
		courseScheduleChangeNotifications.writeAtomicRunResponses(eventId, run, responses, now);
	}

	record AtomicResponse(UUID enrollmentSessionId, UUID memberId, UUID sessionId, Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd, CourseWithdrawalCause cause, UUID responseId, Instant deadline) {
	}

	private Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt) {
		return courseScheduleChangeNotifications.conflictingEnrollmentSessions(runId, sessionId, startAt, endAt);
	}

	private Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt, boolean excludeCurrentRun) {
		return courseScheduleChangeNotifications.conflictingEnrollmentSessions(runId, sessionId, startAt, endAt, excludeCurrentRun);
	}

	private void activeMember(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private final CourseScheduleChangeRepository courseScheduleChangeRepository;

	private final CourseScheduleChangeNotifications courseScheduleChangeNotifications;
}
