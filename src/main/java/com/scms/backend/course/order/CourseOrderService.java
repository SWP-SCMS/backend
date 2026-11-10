package com.scms.backend.course.order;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseOrderException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.CourseStatus;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.course.session.CourseSessionRepository;
import com.scms.backend.scheduling.booking.BookingRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseOrderService {

	private final AccountRepository accounts;

	private final MemberProfileRepository profiles;

	private final CourseRunRepository runs;

	private final CourseSessionRepository sessions;

	private final BookingRepository bookings;

	private final JdbcTemplate db;

	private final AuditEventRepository audits;

	private final CourseReservationExpiryService expiry;

	private final CourseOrderCreationRendezvous creationRendezvous;

	private final Clock clock;

	CourseOrderService(AccountRepository accounts, MemberProfileRepository profiles, CourseRunRepository runs, CourseSessionRepository sessions, BookingRepository bookings, JdbcTemplate db, AuditEventRepository audits, CourseReservationExpiryService expiry, CourseOrderCreationRendezvous creationRendezvous, Clock clock) {
		this.accounts = accounts;
		this.profiles = profiles;
		this.runs = runs;
		this.sessions = sessions;
		this.bookings = bookings;
		this.db = db;
		this.audits = audits;
		this.expiry = expiry;
		this.creationRendezvous = creationRendezvous;
		this.clock = clock;
		this.courseOrderRepository = new CourseOrderRepository(this.db);
	}

	@Transactional
	public CreateResult createForMember(UUID actorId, CourseOrderCreateRequest request, String idempotencyKey) {
		Account actor = lockActive(actorId, AccountRole.MEMBER);
		MemberProfile member = profiles.findById(actor.getId()).orElseThrow(InvalidAuthenticatedAccountException::new);
		return create(actorId, member, request, normalizeKey(idempotencyKey), false);
	}

	@Transactional
	CreateResult createForReceptionist(UUID actorId, UUID memberId, CourseOrderCreateRequest request, String idempotencyKey) {
		lockActive(actorId, AccountRole.RECEPTIONIST);
		MemberProfile member = findMember(memberId);
		lockActiveMember(member.getAccountId());
		return create(actorId, member, request, normalizeKey(idempotencyKey), false);
	}

	public CreateResult createFromWaitlist(UUID actorId, UUID memberId, UUID runId, String idempotencyKey) {
		lockActive(actorId, actorId.equals(memberId) ? AccountRole.MEMBER : AccountRole.RECEPTIONIST);
		lockActiveMember(memberId);
		MemberProfile member = profiles.findById(memberId).orElseThrow(InvalidAuthenticatedAccountException::new);
		return create(actorId, member, new CourseOrderCreateRequest(runId), normalizeKey(idempotencyKey), true);
	}

	@Transactional
	public CourseOrderResponse getForMember(UUID actorId, UUID orderId) {
		lockActive(actorId, AccountRole.MEMBER);
		if (!profiles.existsById(actorId))
			throw new InvalidAuthenticatedAccountException();
		return current(findOrder(orderId, actorId));
	}

	@Transactional
	CourseOrderResponse getForReceptionist(UUID actorId, UUID memberId, UUID orderId) {
		lockActive(actorId, AccountRole.RECEPTIONIST);
		return current(findOrder(orderId, findMember(memberId).getAccountId()));
	}

	static Instant holdExpiresAt(Instant now, Instant registrationClosesAt) {
		Instant fifteenMinutes = now.plusSeconds(900);
		return fifteenMinutes.isBefore(registrationClosesAt) ? fifteenMinutes : registrationClosesAt;
	}

	private CreateResult create(UUID actorId, MemberProfile member, CourseOrderCreateRequest request, String key, boolean invitedSeat) {
		if (request == null || request.courseRunId() == null) {
			throw CourseOrderException.runUnavailable();
		}
		if (member.getAccount().getStatus() != AccountStatus.ACTIVE)
			throw CourseOrderException.memberNotActive();
		StoredOrder replay = findByKey(actorId, key);
		if (replay != null) {
			if (!replay.memberAccountId().equals(member.getAccountId()) || !replay.runId().equals(request.courseRunId()))
				throw CourseOrderException.keyReused();
			return new CreateResult(current(replay), false);
		}
		creationRendezvous.beforeRunLock();
		CourseRun run = runs.findByIdForUpdate(request.courseRunId()).orElseThrow(CourseOrderException::runUnavailable);
		creationRendezvous.afterRunLock();
		Instant now = clock.instant();
		expiry.expireDueForRun(run.getId(), now);
		validateRun(run, now);
		if (courseOrderRepository.countMemberHeldReservations(member.getAccountId(), run.getId(), Timestamp.from(now)) > 0) {
			throw CourseOrderException.reservationExists();
		}
		if (courseOrderRepository.countMemberEnrollments(member.getAccountId(), run.getId()) > 0)
			throw CourseOrderException.alreadyEnrolled();
		long usedSeats = courseOrderRepository.countRunEnrollments(run.getId()) + courseOrderRepository.countRunHeldReservations(run.getId(), Timestamp.from(now)) + courseOrderRepository.createCountCourseWaitlists(run.getId(), Timestamp.from(now)) + courseOrderRepository.createCountCourseTransferHolds(run.getId(), Timestamp.from(now));
		if (usedSeats - (invitedSeat ? 1 : 0) >= run.getCapacity()) {
			throw CourseOrderException.capacityFull();
		}
		validateSchedule(member.getAccountId(), run.getId(), now);
		Instant expiresAt = holdExpiresAt(now, run.getRegistrationClosesAt());
		if (!expiresAt.isAfter(now))
			throw CourseOrderException.registrationClosed();
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase(Locale.ROOT);
		courseOrderRepository.createInsertCourseOrders(orderId, reference, member.getAccountId(), actorId, run.getId(), run.getTuitionVnd(), key, Timestamp.from(now), Timestamp.from(now));
		courseOrderRepository.createInsertCourseReservations(reservationId, orderId, member.getAccountId(), run.getId(), Timestamp.from(expiresAt), Timestamp.from(now), Timestamp.from(now));
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_ORDER_CREATED", "COURSE_ORDER", orderId, Map.of("orderReference", reference, "memberAccountId", member.getAccountId().toString(), "courseRunId", run.getId().toString(), "reservationId", reservationId.toString(), "expiresAt", expiresAt.toString())));
		return new CreateResult(findOrder(orderId, member.getAccountId()).response(), true);
	}

	private CourseOrderResponse current(StoredOrder stored) {
		Instant now = clock.instant();
		if ("HELD".equals(stored.reservationStatus()) && !stored.expiresAt().isAfter(now)) {
			expiry.expireReservation(stored.reservationId(), now);
			return findOrder(stored.response().orderId(), stored.memberAccountId()).response();
		}
		return stored.response();
	}

	private void validateRun(CourseRun run, Instant now) {
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || run.getCourse().getStatus() != CourseStatus.PUBLISHED)
			throw CourseOrderException.runUnavailable();
		if (now.isBefore(run.getRegistrationOpensAt()))
			throw CourseOrderException.registrationNotOpen();
		if (!now.isBefore(run.getRegistrationClosesAt()))
			throw CourseOrderException.registrationClosed();
	}

	private void validateSchedule(UUID memberId, UUID runId, Instant now) {
		for (CourseSession session : sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)) {
			if (session.getStatus() != CourseSessionStatus.SCHEDULED && session.getStatus() != CourseSessionStatus.MAKEUP_SCHEDULED)
				continue;
			if (bookings.existsBookedOverlap(memberId, session.getStartAt(), session.getEndAt()) || hasHeldCourseOverlap(memberId, runId, session, now)) {
				throw CourseOrderException.scheduleConflict();
			}
		}
	}

	private boolean hasHeldCourseOverlap(UUID memberId, UUID runId, CourseSession target, Instant now) {
		return Boolean.TRUE.equals(courseOrderRepository.hasHeldCourseOverlapFindCourseEnrollments(memberId, runId, Timestamp.from(target.getEndAt()), Timestamp.from(target.getStartAt()), memberId, runId, Timestamp.from(now), Timestamp.from(target.getEndAt()), Timestamp.from(target.getStartAt())));
	}

	private Account lockActive(UUID accountId, AccountRole role) {
		return accounts.findByIdForUpdate(accountId).filter(account -> account.getRole() == role && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
	}

	private void lockActiveMember(UUID memberId) {
		Account member = accounts.findByIdForUpdate(memberId).orElseThrow(CourseOrderException::memberNotFound);
		if (member.getRole() != AccountRole.MEMBER)
			throw CourseOrderException.memberNotFound();
		if (member.getStatus() != AccountStatus.ACTIVE)
			throw CourseOrderException.memberNotActive();
	}

	private MemberProfile findMember(UUID memberId) {
		if (memberId == null)
			throw CourseOrderException.memberNotFound();
		return profiles.findById(memberId).filter(profile -> profile.getAccount().getRole() == AccountRole.MEMBER).orElseThrow(CourseOrderException::memberNotFound);
	}

	private String normalizeKey(String key) {
		if (key == null || key.trim().isEmpty() || key.trim().length() > 100) {
			throw CourseOrderException.keyRequired();
		}
		return key.trim();
	}

	private StoredOrder findByKey(UUID actorId, String key) {
		List<StoredOrder> rows = courseOrderRepository.findByKeyFindRows(actorId, key);
		return rows.stream().findFirst().orElse(null);
	}

	private StoredOrder findOrder(UUID orderId, UUID memberId) {
		return courseOrderRepository.findOrderFindRows(orderId, memberId).stream().findFirst().orElseThrow(CourseOrderException::orderNotFound);
	}

	public record CreateResult(CourseOrderResponse response, boolean created) {
	}

	record StoredOrder(CourseOrderResponse response, UUID reservationId, UUID memberAccountId, UUID runId, String reservationStatus, Instant expiresAt) {
	}

	private final CourseOrderRepository courseOrderRepository;
}
