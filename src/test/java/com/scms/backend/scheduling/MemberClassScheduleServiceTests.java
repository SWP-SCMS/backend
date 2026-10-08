package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class MemberClassScheduleServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock SportClassRepository classes;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock SessionLifecycleService lifecycle;
	ClassSessionService service;

	private final Instant now = Instant.parse("2026-10-06T03:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionService(sessions, classes, rooms, accounts, audits, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void memberScheduleShowsClassRoomCoachAndLiveAvailability() {
		UUID memberId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			.thenReturn(true);
		ClassSession session = session(15);
		PageRequest pageable = PageRequest.of(0, 20,
			Sort.by("startTime").ascending().and(Sort.by("id")));
		when(sessions.findAll(any(Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(session), pageable, 1));
		when(sessions.countBookedBySessionIds(List.of(session.getId())))
			.thenReturn(List.of(new ClassSessionRepository.BookingCountView() {
				@Override public UUID getSessionId() { return session.getId(); }
				@Override public long getBookedCount() { return 12; }
			}));

		MemberClassSessionPageResponse result = service.memberSchedule(memberId, null, null, null, pageable);

		assertThat(result.content()).singleElement().satisfies(item -> {
			assertThat(item.className()).isEqualTo("Vinyasa");
			assertThat(item.disciplineName()).isEqualTo("Yoga");
			assertThat(item.coachName()).isEqualTo("Coach");
			assertThat(item.roomName()).isEqualTo("Studio");
			assertThat(item.bookedCount()).isEqualTo(12);
			assertThat(item.availableCapacity()).isEqualTo(3);
			assertThat(item.bookingAvailable()).isTrue();
		});
	}

	@Test
	void availabilityNeverBecomesNegativeOrBookableWhenFull() {
		UUID memberId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			.thenReturn(true);
		ClassSession session = session(15);
		when(sessions.findAll(any(Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(session)));
		when(sessions.countBookedBySessionIds(List.of(session.getId())))
			.thenReturn(List.of(new ClassSessionRepository.BookingCountView() {
				@Override public UUID getSessionId() { return session.getId(); }
				@Override public long getBookedCount() { return 16; }
			}));

		var item = service.memberSchedule(memberId, null, null, null, PageRequest.of(0, 20))
			.content().getFirst();

		assertThat(item.availableCapacity()).isZero();
		assertThat(item.bookingAvailable()).isFalse();
	}

	@Test
	void inactiveMemberCannotViewSchedule() {
		UUID memberId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.memberSchedule(memberId, null, null, null, PageRequest.of(0, 20)))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		verify(sessions, never()).findAll(any(Specification.class), any(PageRequest.class));
	}

	private ClassSession session(int capacity) {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		UUID coachId = UUID.randomUUID();
		Account coach = new Account(coachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"090" + coachId.toString().substring(0, 8), coachId + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach, room, now.plusSeconds(3600),
			now.plusSeconds(7200), capacity, UUID.randomUUID());
	}
}
