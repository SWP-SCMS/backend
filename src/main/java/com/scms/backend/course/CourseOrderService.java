package com.scms.backend.course;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.scheduling.BookingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseOrderService {

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

	CourseOrderService(AccountRepository accounts, MemberProfileRepository profiles, CourseRunRepository runs,
			CourseSessionRepository sessions, BookingRepository bookings, JdbcTemplate db,
			AuditEventRepository audits, CourseReservationExpiryService expiry,
			CourseOrderCreationRendezvous creationRendezvous, Clock clock) {
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
	}

	@Transactional
	CreateResult createForMember(UUID actorId, CourseOrderCreateRequest request, String idempotencyKey) {
		Account actor = lockActive(actorId, AccountRole.MEMBER);
		MemberProfile member = profiles.findById(actor.getId()).orElseThrow(InvalidAuthenticatedAccountException::new);
		return create(actorId, member, request, normalizeKey(idempotencyKey), false);
	}

	@Transactional
	CreateResult createForReceptionist(UUID actorId, UUID memberId, CourseOrderCreateRequest request,
			String idempotencyKey) {
		lockActive(actorId, AccountRole.RECEPTIONIST);
		MemberProfile member = findMember(memberId);
		lockActiveMember(member.getAccountId());
		return create(actorId, member, request, normalizeKey(idempotencyKey), false);
	}

	CreateResult createFromWaitlist(UUID memberId, UUID runId, String idempotencyKey) {
		Account actor = lockActive(memberId, AccountRole.MEMBER);
		MemberProfile member = profiles.findById(actor.getId()).orElseThrow(InvalidAuthenticatedAccountException::new);
		return create(memberId, member, new CourseOrderCreateRequest(runId), normalizeKey(idempotencyKey), true);
	}

	@Transactional
	CourseOrderResponse getForMember(UUID actorId, UUID orderId) {
		lockActive(actorId, AccountRole.MEMBER);
		if (!profiles.existsById(actorId)) throw new InvalidAuthenticatedAccountException();
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

	private CreateResult create(UUID actorId, MemberProfile member, CourseOrderCreateRequest request, String key,
			boolean invitedSeat) {
		if (request == null || request.courseRunId() == null) {
			throw CourseOrderException.runUnavailable();
		}
		if (member.getAccount().getStatus() != AccountStatus.ACTIVE) throw CourseOrderException.memberNotActive();
		StoredOrder replay = findByKey(actorId, key);
		if (replay != null) {
			if (!replay.memberAccountId().equals(member.getAccountId())
					|| !replay.runId().equals(request.courseRunId())) throw CourseOrderException.keyReused();
			return new CreateResult(current(replay), false);
		}

		creationRendezvous.beforeRunLock();
		CourseRun run = runs.findByIdForUpdate(request.courseRunId()).orElseThrow(CourseOrderException::runUnavailable);
		creationRendezvous.afterRunLock();
		Instant now = clock.instant();
		expiry.expireDueForRun(run.getId(), now);
		validateRun(run, now);
		if (count("""
			select count(*) from course_reservations
			where member_account_id=? and course_run_id=? and status='HELD' and expires_at>?
			""", member.getAccountId(), run.getId(), Timestamp.from(now)) > 0) {
			throw CourseOrderException.reservationExists();
		}
		if (count("select count(*) from course_enrollments where member_account_id=? and course_run_id=?",
				member.getAccountId(), run.getId()) > 0) throw CourseOrderException.alreadyEnrolled();
		long usedSeats = count("""
			select count(*) from course_reservations
			where course_run_id=? and (status='ENROLLED' or (status='HELD' and expires_at>?))
			""", run.getId(), Timestamp.from(now)) + count("""
			select count(*) from course_waitlists
			where course_run_id=? and status='INVITED' and invitation_expires_at>?
			""", run.getId(), Timestamp.from(now));
		if (usedSeats - (invitedSeat ? 1 : 0) >= run.getCapacity()) {
			throw CourseOrderException.capacityFull();
		}
		validateSchedule(member.getAccountId(), run.getId(), now);
		Instant expiresAt = holdExpiresAt(now, run.getRegistrationClosesAt());
		if (!expiresAt.isAfter(now)) throw CourseOrderException.registrationClosed();

		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase(Locale.ROOT);
		db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,
				tuition_vnd_snapshot,currency_code,status,idempotency_key,created_at,updated_at)
			values (?,?,?,?,?,?,'VND','PENDING_PAYMENT',?,?,?)
			""", orderId, reference, member.getAccountId(), actorId, run.getId(), run.getTuitionVnd(), key,
			Timestamp.from(now), Timestamp.from(now));
		db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,
				created_at,updated_at)
			values (?,?,?,?,'HELD',?,?,?)
			""", reservationId, orderId, member.getAccountId(), run.getId(), Timestamp.from(expiresAt),
			Timestamp.from(now), Timestamp.from(now));
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_ORDER_CREATED", "COURSE_ORDER", orderId,
			Map.of("orderReference", reference, "memberAccountId", member.getAccountId().toString(),
				"courseRunId", run.getId().toString(), "reservationId", reservationId.toString(),
				"expiresAt", expiresAt.toString())));
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
		if (run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION
				|| run.getCourse().getStatus() != CourseStatus.PUBLISHED) throw CourseOrderException.runUnavailable();
		if (now.isBefore(run.getRegistrationOpensAt())) throw CourseOrderException.registrationNotOpen();
		if (!now.isBefore(run.getRegistrationClosesAt())) throw CourseOrderException.registrationClosed();
	}

	private void validateSchedule(UUID memberId, UUID runId, Instant now) {
		for (CourseSession session : sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)) {
			if (session.getStatus() != CourseSessionStatus.SCHEDULED
					&& session.getStatus() != CourseSessionStatus.MAKEUP_SCHEDULED) continue;
			if (bookings.existsBookedOverlap(memberId, session.getStartAt(), session.getEndAt())
					|| hasHeldCourseOverlap(memberId, runId, session, now)) {
				throw CourseOrderException.scheduleConflict();
			}
		}
	}

	private boolean hasHeldCourseOverlap(UUID memberId, UUID runId, CourseSession target, Instant now) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists (
				select 1 from course_reservations reservation
				join course_sessions session on session.course_run_id=reservation.course_run_id
				where reservation.member_account_id=? and reservation.course_run_id<>?
				  and (reservation.status='ENROLLED' or (reservation.status='HELD' and reservation.expires_at>?))
				  and session.status in ('SCHEDULED','MAKEUP_SCHEDULED')
				  and session.start_at<? and session.end_at>?
			)
			""", Boolean.class, memberId, runId, Timestamp.from(now), Timestamp.from(target.getEndAt()),
			Timestamp.from(target.getStartAt())));
	}

	private Account lockActive(UUID accountId, AccountRole role) {
		return accounts.findByIdForUpdate(accountId)
			.filter(account -> account.getRole() == role && account.getStatus() == AccountStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedAccountException::new);
	}

	private void lockActiveMember(UUID memberId) {
		Account member = accounts.findByIdForUpdate(memberId).orElseThrow(CourseOrderException::memberNotFound);
		if (member.getRole() != AccountRole.MEMBER) throw CourseOrderException.memberNotFound();
		if (member.getStatus() != AccountStatus.ACTIVE) throw CourseOrderException.memberNotActive();
	}

	private MemberProfile findMember(UUID memberId) {
		if (memberId == null) throw CourseOrderException.memberNotFound();
		return profiles.findById(memberId).filter(profile -> profile.getAccount().getRole() == AccountRole.MEMBER)
			.orElseThrow(CourseOrderException::memberNotFound);
	}

	private String normalizeKey(String key) {
		if (key == null || key.trim().isEmpty() || key.trim().length() > 100) {
			throw CourseOrderException.keyRequired();
		}
		return key.trim();
	}

	private StoredOrder findByKey(UUID actorId, String key) {
		List<StoredOrder> rows = db.query(orderQuery() + " where orders.actor_account_id=? and orders.idempotency_key=?",
			this::map, actorId, key);
		return rows.stream().findFirst().orElse(null);
	}

	private StoredOrder findOrder(UUID orderId, UUID memberId) {
		return db.query(orderQuery() + " where orders.id=? and orders.member_account_id=?", this::map,
			orderId, memberId).stream().findFirst().orElseThrow(CourseOrderException::orderNotFound);
	}

	private String orderQuery() {
		return """
			select orders.id,orders.order_reference,orders.member_account_id,
				orders.course_run_id,orders.tuition_vnd_snapshot,orders.currency_code,orders.status order_status,
				reservation.id reservation_id,reservation.status reservation_status,reservation.expires_at,
				orders.created_at,payment.id payment_id,payment.amount payment_amount,
				payment.currency_code payment_currency,payment.method payment_method,payment.provider,
				payment.payment_reference,payment.bank_code_snapshot,payment.bank_account_number_snapshot,
				payment.bank_account_name_snapshot,payment.status payment_status,payment.deadline_at payment_deadline
			from course_orders orders
			join course_reservations reservation on reservation.course_order_id=orders.id
			left join course_payments payment on payment.course_order_id=orders.id
			""";
	}

	private StoredOrder map(ResultSet result, int row) throws SQLException {
		UUID orderId = result.getObject("id", UUID.class);
		UUID memberId = result.getObject("member_account_id", UUID.class);
		UUID runId = result.getObject("course_run_id", UUID.class);
		Instant expiresAt = result.getTimestamp("expires_at").toInstant();
		String reservationStatus = result.getString("reservation_status");
		UUID paymentId = result.getObject("payment_id", UUID.class);
		CoursePaymentResponse payment = paymentId == null ? null : new CoursePaymentResponse(paymentId, orderId,
			result.getBigDecimal("payment_amount").toBigIntegerExact(), result.getString("payment_currency").trim(),
			result.getString("payment_method"), result.getString("provider"), result.getString("payment_reference"),
			result.getString("bank_code_snapshot"), result.getString("bank_account_number_snapshot"),
			result.getString("bank_account_name_snapshot"), "PENDING".equals(result.getString("payment_status"))
				? CoursePaymentResponse.qrUrl(
				result.getString("bank_code_snapshot"), result.getString("bank_account_number_snapshot"),
				result.getString("bank_account_name_snapshot"), result.getBigDecimal("payment_amount").toBigIntegerExact(),
				result.getString("payment_reference")) : null, result.getTimestamp("payment_deadline").toInstant(),
			result.getString("payment_status"));
		return new StoredOrder(new CourseOrderResponse(orderId, result.getString("order_reference"),
			memberId, runId, result.getBigDecimal("tuition_vnd_snapshot").toBigIntegerExact(),
			result.getString("currency_code").trim(), result.getString("order_status"), reservationStatus,
			expiresAt, result.getTimestamp("created_at").toInstant(), payment), result.getObject("reservation_id", UUID.class),
			memberId, runId, reservationStatus, expiresAt);
	}

	private long count(String sql, Object... arguments) {
		return db.queryForObject(sql, Long.class, arguments);
	}

	record CreateResult(CourseOrderResponse response, boolean created) { }

	private record StoredOrder(CourseOrderResponse response, UUID reservationId, UUID memberAccountId,
		UUID runId, String reservationStatus, Instant expiresAt) { }
}
