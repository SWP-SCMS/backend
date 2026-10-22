package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.training.CoachAssignment;
import com.scms.backend.training.CoachAssignmentRepository;
import com.scms.backend.training.CoachAssignmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class BookingServiceTests {

	@Mock BookingRepository bookings;
	@Mock ClassSessionRepository sessions;
	@Mock DisciplineRepository disciplines;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock NotificationWriter notifications;
	@Mock SessionLifecycleService lifecycle;
	@Mock CoachAssignmentRepository coachAssignments;
	@Mock WaitlistPromotionService waitlistPromotions;
	BookingService service;

	private final Instant now = Instant.parse("2026-10-06T03:00:00Z");
	private UUID memberId;
	private UUID receptionistId;
	private ClassSession session;

	@BeforeEach
	void setUp() {
		service = new BookingService(bookings, sessions, disciplines, accounts, audits, notifications, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC), coachAssignments, waitlistPromotions);
		memberId = UUID.randomUUID();
		receptionistId = UUID.randomUUID();
		Account member = account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member");
		org.mockito.Mockito.lenient().when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
		org.mockito.Mockito.lenient().when(accounts.findByIdForUpdate(receptionistId)).thenReturn(Optional.of(
			account(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE, "Receptionist")));
		session = session();
		org.mockito.Mockito.lenient().when(sessions.findByIdForUpdate(session.getId()))
			.thenReturn(Optional.of(session));
		org.mockito.Mockito.lenient().when(disciplines.findBySessionIdForUpdate(session.getId()))
			.thenReturn(Optional.of(session.getSportClass().getDiscipline()));
	}

	@Test
	void locksSessionDisciplineBeforeComputingEligibility() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.saveAndFlush(org.mockito.ArgumentMatchers.any(Booking.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.book(memberId, session.getId());

		var order = inOrder(sessions, disciplines, bookings);
		order.verify(sessions).findByIdForUpdate(session.getId());
		order.verify(disciplines).findBySessionIdForUpdate(session.getId());
		order.verify(bookings).findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime());
	}

	@Test
	void bookingUsesSharedDisciplineLockWhilePatchRetainsExclusiveLock() throws Exception {
		Method bookingLock = DisciplineRepository.class.getMethod("findBySessionIdForUpdate", UUID.class);
		Method patchLock = DisciplineRepository.class.getMethod("findByIdForUpdate", UUID.class);

		assertThat(bookingLock.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_READ);
		assertThat(patchLock.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test
	void booksWithMembershipBenefitsCoveringSessionStart() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.saveAndFlush(org.mockito.ArgumentMatchers.any(Booking.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		BookingResponse response = service.book(memberId, session.getId());

		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.sessionId()).isEqualTo(session.getId());
		assertThat(response.membershipId()).isEqualTo(membershipId);
		assertThat(response.status()).isEqualTo(BookingStatus.BOOKED);
		verify(lifecycle).catchUp(now);
		verify(audits).save(org.mockito.ArgumentMatchers.any());
		verify(notifications).write("BOOKING_CREATED:" + response.id(), memberId, "BOOKING_CREATED", "BOOKING",
			response.id(), java.util.Map.of("sessionId", session.getId().toString()));
	}

	@Test
	void membershipMustProvideRequiredBenefitsAndCoverSessionStart() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(
				eligibility(membershipId, "CLASS_BOOKING", true),
				eligibility(membershipId, "POOL_ACCESS", false)));

		assertThatThrownBy(() -> service.book(memberId, session.getId()))
			.isInstanceOfSatisfying(BookingException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("MEMBERSHIP_BENEFIT_REQUIRED");
				assertThat(exception.getRequiredBenefitCodes())
					.containsExactly("CLASS_BOOKING", "POOL_ACCESS");
				assertThat(exception.getMissingBenefitCodes()).containsExactly("POOL_ACCESS");
			});
	}

	@Test
	void receptionistReceivesTheSameStructuredBenefitFailure() {
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(null, "CLASS_BOOKING", false)));

		assertThatThrownBy(() -> service.bookForMember(receptionistId, memberId, session.getId()))
			.isInstanceOfSatisfying(BookingException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("MEMBERSHIP_BENEFIT_REQUIRED");
				assertThat(exception.getRequiredBenefitCodes()).containsExactly("CLASS_BOOKING");
				assertThat(exception.getMissingBenefitCodes()).containsExactly("CLASS_BOOKING");
			});
	}

	@Test
	void duplicateBookedSessionIsRejected() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, session.getId(),
			BookingStatus.BOOKED)).thenReturn(true);

		assertCode("BOOKING_DUPLICATE");
	}

	@Test
	void overlappingBookingIsRejected() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.existsBookedOverlap(memberId, session.getStartTime(), session.getEndTime())).thenReturn(true);

		assertCode("BOOKING_TIME_CONFLICT");
	}

	@Test
	void fullSessionIsRejected() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.countByClassSessionIdAndStatus(session.getId(), BookingStatus.BOOKED))
			.thenReturn((long) session.getCapacity());

		assertCode("SESSION_FULL");
	}

	@Test
	void ptBookingRequiresMembersActiveAssignedCoach() {
		ClassSession pt = ptSession();
		when(sessions.findByIdForUpdate(pt.getId())).thenReturn(Optional.of(pt));

		assertThatThrownBy(() -> service.book(memberId, pt.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo("PT_COACH_ASSIGNMENT_MISMATCH");
	}

	@Test
	void ptBookingWithAssignedCoachUsesNormalEligibilityAndCapacityRules() {
		ClassSession pt = ptSession(); UUID membershipId = UUID.randomUUID();
		when(sessions.findByIdForUpdate(pt.getId())).thenReturn(Optional.of(pt));
		when(coachAssignments.findByMemberAccountIdAndCoachAccountIdAndStatus(memberId,
			pt.getTeachingCoach().getId(), CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.of(
				new CoachAssignment(UUID.randomUUID(), memberId, pt.getTeachingCoach().getId(), UUID.randomUUID(),
					CoachAssignmentStatus.ACTIVE, now.minusSeconds(60), now.minusSeconds(60))));
		when(disciplines.findBySessionIdForUpdate(pt.getId())).thenReturn(Optional.of(pt.getSportClass().getDiscipline()));
		when(bookings.findBookingBenefitEligibility(memberId, pt.getId(), pt.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.book(memberId, pt.getId()).status()).isEqualTo(BookingStatus.BOOKED);
	}

	@Test
	void cancellationAtExactlyTwoHoursIsAcceptedAndKeepsHistory() {
		ClassSession cancellableSession = sessionAt(now.plusSeconds(7200));
		Booking booking = booking(cancellableSession);
		when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
		when(sessions.findByIdForUpdate(cancellableSession.getId())).thenReturn(Optional.of(cancellableSession));
		when(bookings.saveAndFlush(booking)).thenReturn(booking);

		BookingResponse response = service.cancel(memberId, booking.getId());

		assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(response.cancelledAt()).isEqualTo(now);
		assertThat(response.cancellationSource()).isEqualTo("MEMBER");
		verify(audits).save(org.mockito.ArgumentMatchers.any());
		verify(notifications).write("BOOKING_CANCELLED:" + booking.getId(), memberId, "BOOKING_CANCELLED", "BOOKING",
			booking.getId(), java.util.Map.of("sessionId", cancellableSession.getId().toString()));
		verify(waitlistPromotions).promoteAvailable(cancellableSession);
	}

	@Test
	void cancellationBelowTwoHoursIsRejected() {
		ClassSession lateSession = sessionAt(now.plusSeconds(7199));
		Booking booking = booking(lateSession);
		when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
		when(sessions.findByIdForUpdate(lateSession.getId())).thenReturn(Optional.of(lateSession));

		assertThatThrownBy(() -> service.cancel(memberId, booking.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo("BOOKING_CANCELLATION_WINDOW_CLOSED");
	}

	@Test
	void memberCannotCancelAnotherMembersBooking() {
		Booking booking = new Booking(UUID.randomUUID(), session.getId(), UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), now.minusSeconds(60));
		when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

		assertThatThrownBy(() -> service.cancel(memberId, booking.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo("BOOKING_NOT_FOUND");
	}

	@Test
	void receptionistBooksForMemberWithoutBypassingMemberRules() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findBookingBenefitEligibility(memberId, session.getId(), session.getStartTime()))
			.thenReturn(List.of(eligibility(membershipId, "CLASS_BOOKING", true)));
		when(bookings.saveAndFlush(org.mockito.ArgumentMatchers.any(Booking.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		BookingResponse response = service.bookForMember(receptionistId, memberId, session.getId());

		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.bookedBy()).isEqualTo(receptionistId);
		ArgumentCaptor<com.scms.backend.audit.AuditEvent> audit =
			ArgumentCaptor.forClass(com.scms.backend.audit.AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(receptionistId);
	}

	@Test
	void inactiveReceptionistCannotBookForMember() {
		when(accounts.findByIdForUpdate(receptionistId)).thenReturn(Optional.of(
			account(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.INACTIVE, "Receptionist")));

		assertThatThrownBy(() -> service.bookForMember(receptionistId, memberId, session.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo("BOOKING_ACTOR_NOT_ACTIVE_RECEPTIONIST");
	}

	@Test
	void nonReceptionistCannotBookForMember() {
		when(accounts.findByIdForUpdate(receptionistId)).thenReturn(Optional.of(
			account(receptionistId, AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager")));

		assertThatThrownBy(() -> service.bookForMember(receptionistId, memberId, session.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo("BOOKING_ACTOR_NOT_ACTIVE_RECEPTIONIST");
	}

	@Test
	void receptionistCancelsForMemberAndRemainsAuditActor() {
		ClassSession cancellableSession = sessionAt(now.plusSeconds(7200));
		Booking booking = booking(cancellableSession);
		when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
		when(sessions.findByIdForUpdate(cancellableSession.getId())).thenReturn(Optional.of(cancellableSession));
		when(bookings.saveAndFlush(booking)).thenReturn(booking);

		BookingResponse response = service.cancelForMember(receptionistId, memberId, booking.getId());

		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.cancelledBy()).isEqualTo(receptionistId);
		assertThat(response.cancellationSource()).isEqualTo("RECEPTIONIST");
		ArgumentCaptor<com.scms.backend.audit.AuditEvent> audit =
			ArgumentCaptor.forClass(com.scms.backend.audit.AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(receptionistId);
	}

	@Test
	void receptionistCannotCancelAnotherMembersBooking() {
		Booking booking = new Booking(UUID.randomUUID(), session.getId(), UUID.randomUUID(), UUID.randomUUID(),
			receptionistId, now.minusSeconds(60));
		when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

		assertThatThrownBy(() -> service.cancelForMember(receptionistId, memberId, booking.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo("BOOKING_NOT_FOUND");
	}

	@Test
	void memberBookingScheduleIsLimitedToTheAuthenticatedMember() {
		Booking booking = booking(session);
		when(bookings.findByMemberAccountId(memberId, PageRequest.of(0, 20)))
			.thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 20), 1));
		when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

		BookingSchedulePageResponse response = service.listForMember(memberId, null, PageRequest.of(0, 20));

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().memberId()).isEqualTo(memberId);
		assertThat(response.content().getFirst().sessionId()).isEqualTo(session.getId());
	}

	@Test
	void receptionistScheduleCanFilterBookingStatusForRequestedMember() {
		when(accounts.findById(memberId)).thenReturn(Optional.of(
			account(memberId, AccountRole.MEMBER, AccountStatus.SUSPENDED, "Member")));
		Booking booking = booking(session);
		when(bookings.findByMemberAccountIdAndStatus(memberId, BookingStatus.BOOKED, PageRequest.of(0, 20)))
			.thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 20), 1));
		when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

		BookingSchedulePageResponse response = service.listForReceptionist(receptionistId, memberId,
			BookingStatus.BOOKED, PageRequest.of(0, 20));

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().status()).isEqualTo(BookingStatus.BOOKED);
		verify(bookings).findByMemberAccountIdAndStatus(memberId, BookingStatus.BOOKED, PageRequest.of(0, 20));
	}

	private void assertCode(String code) {
		assertThatThrownBy(() -> service.book(memberId, session.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo(code);
	}

	private ClassSession session() {
		return sessionAt(now.plusSeconds(3600));
	}

	private ClassSession sessionAt(Instant start) {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		Account coach = account(UUID.randomUUID(), AccountRole.COACH, AccountStatus.ACTIVE, "Coach");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach, room, start,
			start.plusSeconds(3600), 15, UUID.randomUUID());
	}

	private Booking booking(ClassSession targetSession) {
		return new Booking(UUID.randomUUID(), targetSession.getId(), memberId, UUID.randomUUID(), memberId,
			now.minusSeconds(60));
	}

	private ClassSession ptSession() {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Personal training", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "PT", SportClassType.PT_1_1,
			null, SportClassStatus.ACTIVE);
		Account coach = account(UUID.randomUUID(), AccountRole.COACH, AccountStatus.ACTIVE, "Coach");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach, null, now.plusSeconds(3600),
			now.plusSeconds(7200), 1, UUID.randomUUID());
	}

	private Account account(UUID id, AccountRole role, AccountStatus status, String name) {
		return new Account(id, role, status, name, "09" + id.toString().replace("-", "").substring(0, 8),
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
	}

	private BookingRepository.BookingBenefitEligibilityView eligibility(UUID membershipId, String code,
			boolean provided) {
		return new BookingRepository.BookingBenefitEligibilityView() {
			@Override public UUID getMembershipId() { return membershipId; }
			@Override public String getRequiredBenefitCode() { return code; }
			@Override public boolean getProvided() { return provided; }
		};
	}
}
