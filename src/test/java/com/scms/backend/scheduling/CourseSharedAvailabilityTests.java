package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.CourseSessionRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;

class CourseSharedAvailabilityTests {
	@Test
	void roomImpactPreviewCountsAndIdentifiesCourseSessions() {
		UUID courseSessionId = UUID.randomUUID();
		RoomImpactPreviewResponse response = RoomImpactPreviewResponse.from(List.of(), java.util.Map.of(),
			java.util.Map.of(), List.of(), List.of(courseSessionId));

		assertThat(response.count()).isOne();
		assertThat(response.courseSessionIds()).containsExactly(courseSessionId);
	}

	@Test
	void gymSessionRejectsOverlappingCourseResource() {
		CourseSessionRepository courseSessions = mock(CourseSessionRepository.class);
		ClassSessionService service = new ClassSessionService(mock(ClassSessionRepository.class),
			mock(SportClassRepository.class), mock(RoomRepository.class), mock(AccountRepository.class),
			mock(AuditEventRepository.class), mock(SessionLifecycleService.class), Clock.systemUTC(),
			null, null, null, null, null, null, courseSessions, null);
		Account coach = mock(Account.class); Room room = mock(Room.class);
		UUID coachId = UUID.randomUUID(); UUID roomId = UUID.randomUUID();
		when(coach.getId()).thenReturn(coachId); when(room.getId()).thenReturn(roomId);
		Instant start = Instant.parse("2035-01-01T02:00:00Z");
		when(courseSessions.existsResourceOverlap(coachId, roomId, start, start.plusSeconds(3600)))
			.thenReturn(true);

		assertThatThrownBy(() -> service.ensureAvailable(new ClassSessionService.Resources(null, coach, room),
			start, start.plusSeconds(3600), 10, null))
			.isInstanceOf(RecurringScheduleException.class);
	}

	@Test
	void coachLeaveIncludesCourseSessionImpacts() {
		CourseSessionRepository courseSessions = mock(CourseSessionRepository.class);
		ClassSessionRepository gymSessions = mock(ClassSessionRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		CoachLeaveRepository leaves = mock(CoachLeaveRepository.class);
		UUID managerId = UUID.randomUUID(), coachId = UUID.randomUUID(), requestId = UUID.randomUUID();
		Account manager = mock(Account.class), coach = mock(Account.class);
		CoachLeaveRequest leave = mock(CoachLeaveRequest.class);
		Instant start = Instant.parse("2035-01-01T02:00:00Z"), end = start.plusSeconds(3600);
		when(accounts.findById(managerId)).thenReturn(java.util.Optional.of(manager));
		when(manager.getRole()).thenReturn(com.scms.backend.account.AccountRole.MANAGER);
		when(manager.getStatus()).thenReturn(com.scms.backend.account.AccountStatus.ACTIVE);
		when(accounts.findByIdForUpdate(coachId)).thenReturn(java.util.Optional.of(coach));
		when(leaves.findByIdForUpdate(requestId)).thenReturn(java.util.Optional.of(leave));
		when(leave.getId()).thenReturn(requestId); when(leave.getCoach()).thenReturn(coach);
		when(coach.getId()).thenReturn(coachId); when(leave.getStartTime()).thenReturn(start);
		when(leave.getEndTime()).thenReturn(end); when(leave.getStatus()).thenReturn(CoachLeaveStatus.PENDING);
		when(courseSessions.findActiveOverlapIdsByCoach(coachId, start, end)).thenReturn(List.of(UUID.randomUUID()));
		CoachLeaveService service = new CoachLeaveService(leaves, accounts, gymSessions,
			mock(AuditEventRepository.class), mock(NotificationWriter.class), null,
			Clock.fixed(Instant.parse("2034-01-01T00:00:00Z"), ZoneOffset.UTC), courseSessions);

		assertThatThrownBy(() -> service.approve(managerId, requestId, new CoachLeaveReviewRequest("review")))
			.isInstanceOf(CoachLeaveException.class);
	}

	@Test
	void roomCapacityCannotDropBelowAnActiveCourseRun() {
		RoomRepository rooms = mock(RoomRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		CourseSessionRepository courseSessions = mock(CourseSessionRepository.class);
		UUID managerId = UUID.randomUUID(), roomId = UUID.randomUUID();
		Room room = mock(Room.class);
		when(accounts.existsByIdAndRoleAndStatus(managerId, com.scms.backend.account.AccountRole.MANAGER,
			com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		when(rooms.findByIdForUpdate(roomId)).thenReturn(java.util.Optional.of(room));
		when(room.getName()).thenReturn("Course room"); when(room.getCapacity()).thenReturn(30);
		when(room.getStatus()).thenReturn(RoomStatus.ACTIVE); when(room.getUsageMode()).thenReturn(RoomUsageMode.EXCLUSIVE);
		when(courseSessions.existsRunOverRoomCapacity(roomId, 20)).thenReturn(true);
		RoomService service = new RoomService(rooms, accounts, mock(AuditEventRepository.class), courseSessions);

		assertThatThrownBy(() -> service.update(managerId, roomId, new RoomPatchRequest(null, 20, null)))
			.isInstanceOf(RoomException.class);
	}
}
