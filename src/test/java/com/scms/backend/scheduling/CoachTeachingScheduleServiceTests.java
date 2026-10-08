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
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CoachTeachingScheduleServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock BookingRepository bookings;
	@Mock AccountRepository accounts;
	@Mock SessionLifecycleService lifecycle;
	CoachTeachingScheduleService service;

	private final Instant now = Instant.parse("2026-10-06T08:00:00Z");

	@BeforeEach
	void setUp() {
		service = new CoachTeachingScheduleService(sessions, bookings, accounts, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void activeCoachCanListOnlyTheirTeachingSessions() {
		ClassSession session = session();
		UUID coachId = session.getTeachingCoach().getId();
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(sessions.findAll(any(Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(session)));

		ClassSessionPageResponse result = service.list(coachId, null, null, null,
			PageRequest.of(0, 20));

		assertThat(result.content()).singleElement()
			.extracting(ClassSessionResponse::coachId).isEqualTo(coachId);
		verify(lifecycle).catchUp(now);
	}

	@Test
	void coachCannotReadAnotherCoachsSession() {
		ClassSession session = session();
		UUID coachId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(coachId, session.getId()))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_NOT_FOUND");
		verify(bookings, never()).findBookedMembersBySessionId(any());
	}

	@Test
	void detailIncludesOnlyCurrentlyBookedMembers() {
		ClassSession session = session();
		UUID coachId = session.getTeachingCoach().getId();
		UUID memberId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(bookings.findBookedMembersBySessionId(session.getId())).thenReturn(List.of(
			new BookingRepository.BookedMemberView() {
				@Override public UUID getMemberId() { return memberId; }
				@Override public String getMemberCode() { return "MB-37"; }
				@Override public String getFullName() { return "Member Test"; }
				@Override public String getPhone() { return "0900000037"; }
				@Override public String getProfileImageUrl() { return "https://example.test/member.jpg"; }
			}));

		CoachClassSessionDetailResponse result = service.get(coachId, session.getId());

		assertThat(result.id()).isEqualTo(session.getId());
		assertThat(result.members()).singleElement().satisfies(member -> {
			assertThat(member.memberId()).isEqualTo(memberId);
			assertThat(member.memberCode()).isEqualTo("MB-37");
			assertThat(member.fullName()).isEqualTo("Member Test");
		});
	}

	@Test
	void inactiveCoachCannotViewTeachingSchedule() {
		UUID coachId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.list(coachId, null, null, null, PageRequest.of(0, 20)))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		verify(sessions, never()).findAll(any(Specification.class), any(PageRequest.class));
	}

	private ClassSession session() {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		UUID coachId = UUID.randomUUID();
		Account coach = new Account(coachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"090" + coachId.toString().replace("-", "").substring(0, 8), coachId + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach, room, now.plusSeconds(3600),
			now.plusSeconds(7200), 15, UUID.randomUUID());
	}
}
