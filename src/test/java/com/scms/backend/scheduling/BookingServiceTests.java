package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BookingServiceTests {

	@Mock BookingRepository bookings;
	@Mock ClassSessionRepository sessions;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock ApplicationEventPublisher events;
	@Mock SessionLifecycleService lifecycle;
	BookingService service;

	private final Instant now = Instant.parse("2026-10-06T03:00:00Z");
	private UUID memberId;
	private UUID receptionistId;
	private ClassSession session;

	@BeforeEach
	void setUp() {
		service = new BookingService(bookings, sessions, accounts, audits, events, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC));
		memberId = UUID.randomUUID();
		receptionistId = UUID.randomUUID();
		Account member = account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member");
		org.mockito.Mockito.lenient().when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
		org.mockito.Mockito.lenient().when(accounts.findByIdForUpdate(receptionistId)).thenReturn(Optional.of(
			account(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE, "Receptionist")));
		session = session();
		org.mockito.Mockito.lenient().when(sessions.findByIdForUpdate(session.getId()))
			.thenReturn(Optional.of(session));
	}

	@Test
	void booksWithPlusMembershipCoveringSessionStart() {
		UUID membershipId = UUID.randomUUID();
		when(bookings.findEligiblePlusMembership(memberId, session.getStartTime()))
			.thenReturn(Optional.of(membershipId));
		when(bookings.saveAndFlush(org.mockito.ArgumentMatchers.any(Booking.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		BookingResponse response = service.book(memberId, session.getId());

		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.sessionId()).isEqualTo(session.getId());
		assertThat(response.membershipId()).isEqualTo(membershipId);
		assertThat(response.status()).isEqualTo(BookingStatus.BOOKED);
		verify(lifecycle).catchUp(now);
		verify(audits).save(org.mockito.ArgumentMatchers.any());
		ArgumentCaptor<BookingCreatedEvent> event = ArgumentCaptor.forClass(BookingCreatedEvent.class);
		verify(events).publishEvent(event.capture());
		assertThat(event.getValue().memberId()).isEqualTo(memberId);
	}

	@Test
	void plusMembershipMustCoverSessionStart() {
		when(bookings.findEligiblePlusMembership(memberId, session.getStartTime())).thenReturn(Optional.empty());

		assertCode("BOOKING_PLUS_MEMBERSHIP_REQUIRED");
	}

	@Test
	void duplicateBookedSessionIsRejected() {
		when(bookings.findEligiblePlusMembership(memberId, session.getStartTime()))
			.thenReturn(Optional.of(UUID.randomUUID()));
		when(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, session.getId(),
			BookingStatus.BOOKED)).thenReturn(true);

		assertCode("BOOKING_DUPLICATE");
	}

	@Test
	void overlappingBookingIsRejected() {
		when(bookings.findEligiblePlusMembership(memberId, session.getStartTime()))
			.thenReturn(Optional.of(UUID.randomUUID()));
		when(bookings.existsBookedOverlap(memberId, session.getStartTime(), session.getEndTime())).thenReturn(true);

		assertCode("BOOKING_TIME_CONFLICT");
	}

	@Test
	void fullSessionIsRejected() {
		when(bookings.findEligiblePlusMembership(memberId, session.getStartTime()))
			.thenReturn(Optional.of(UUID.randomUUID()));
		when(bookings.countByClassSessionIdAndStatus(session.getId(), BookingStatus.BOOKED))
			.thenReturn((long) session.getCapacity());

		assertCode("SESSION_FULL");
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
		verify(events).publishEvent(org.mockito.ArgumentMatchers.any(BookingCancelledEvent.class));
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
		when(bookings.findEligiblePlusMembership(memberId, session.getStartTime()))
			.thenReturn(Optional.of(membershipId));
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

	private Account account(UUID id, AccountRole role, AccountStatus status, String name) {
		return new Account(id, role, status, name, "09" + id.toString().replace("-", "").substring(0, 8),
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
	}
}
