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
	BookingService service;

	private final Instant now = Instant.parse("2026-10-06T03:00:00Z");
	private UUID memberId;
	private ClassSession session;

	@BeforeEach
	void setUp() {
		service = new BookingService(bookings, sessions, accounts, audits, events,
			Clock.fixed(now, ZoneOffset.UTC));
		memberId = UUID.randomUUID();
		Account member = account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member");
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
		session = session();
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
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

	private void assertCode(String code) {
		assertThatThrownBy(() -> service.book(memberId, session.getId()))
			.isInstanceOf(BookingException.class)
			.extracting("code").isEqualTo(code);
	}

	private ClassSession session() {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		Account coach = account(UUID.randomUUID(), AccountRole.COACH, AccountStatus.ACTIVE, "Coach");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach, room, now.plusSeconds(3600),
			now.plusSeconds(7200), 15, UUID.randomUUID());
	}

	private Account account(UUID id, AccountRole role, AccountStatus status, String name) {
		return new Account(id, role, status, name, "09" + id.toString().replace("-", "").substring(0, 8),
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
	}
}
