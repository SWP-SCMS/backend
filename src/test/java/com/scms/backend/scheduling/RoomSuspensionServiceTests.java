package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomSuspensionServiceTests {

	@Mock RoomSuspensionRepository suspensions;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock ClassSessionRepository sessions;
	@Mock ClassSessionService sessionService;
	@Mock ClassSessionCancellationService cancellationService;
	@Mock RoomService roomService;
	@Mock SessionLifecycleService lifecycle;
	@Mock RoomImpactLockRendezvous lockRendezvous;
	RoomSuspensionService service;

	private final Instant now = Instant.parse("2026-10-05T00:00:00Z");

	@BeforeEach
	void setUp() {
		service = new RoomSuspensionService(suspensions, rooms, accounts, audits,
			Clock.fixed(now, ZoneOffset.UTC), sessions, sessionService, cancellationService,
			roomService, lifecycle, (JdbcTemplate) null, (NotificationWriter) null, lockRendezvous);
	}

	@Test
	void createsFutureSuspensionWhenRoomHasNoConflict() {
		UUID managerId = activeManager();
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(suspensions.existsOverlap(any(), any(), any())).thenReturn(false);
		when(rooms.existsActiveSessionOverlap(any(), any(), any())).thenReturn(false);
		when(suspensions.saveAndFlush(any(RoomSuspension.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RoomSuspensionResponse result = service.create(managerId, room.getId(),
			new RoomSuspensionCreateRequest(now.plusSeconds(3600), now.plusSeconds(7200), "Repair"));

		assertThat(result.roomId()).isEqualTo(room.getId());
		assertThat(result.reason()).isEqualTo("Repair");
	}

	@Test
	void rejectsSuspensionOverlappingSession() {
		UUID managerId = activeManager();
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(suspensions.existsOverlap(any(), any(), any())).thenReturn(false);
		when(rooms.existsActiveSessionOverlap(any(), any(), any())).thenReturn(true);

		assertThatThrownBy(() -> service.create(managerId, room.getId(),
			new RoomSuspensionCreateRequest(now.plusSeconds(3600), now.plusSeconds(7200), null)))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_SUSPENSION_CONFLICT");
	}

	@Test
	void locksRoomBeforeCheckingOverlaps() {
		UUID managerId = activeManager();
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(suspensions.saveAndFlush(any(RoomSuspension.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.create(managerId, room.getId(),
			new RoomSuspensionCreateRequest(now.plusSeconds(3600), now.plusSeconds(7200), null));

		InOrder ordered = inOrder(rooms, suspensions);
		ordered.verify(rooms).findByIdForUpdate(room.getId());
		ordered.verify(suspensions).existsOverlap(room.getId(), now.plusSeconds(3600), now.plusSeconds(7200));
		ordered.verify(rooms).existsActiveSessionOverlap(room.getId(), now.plusSeconds(3600), now.plusSeconds(7200));
	}

	@Test
	void normalizesMoveActionBeforeLockingAndLocksAllRoomsInUuidOrder() {
		UUID managerId = activeManager();
		UUID targetId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Room target = new Room(targetId, "Target", 20, RoomStatus.ACTIVE);
		Room source = new Room(sourceId, "Source", 20, RoomStatus.ACTIVE);
		ClassSession session = org.mockito.Mockito.mock(ClassSession.class);
		UUID sessionId = UUID.randomUUID();
		com.scms.backend.account.Account coach = org.mockito.Mockito.mock(com.scms.backend.account.Account.class);
		when(session.getId()).thenReturn(sessionId);
		when(session.getStatus()).thenReturn(ClassSessionStatus.SCHEDULED);
		when(session.getCapacity()).thenReturn(10);
		when(session.getTeachingCoach()).thenReturn(coach);
		when(session.getStartTime()).thenReturn(now.plusSeconds(3600));
		when(session.getEndTime()).thenReturn(now.plusSeconds(7200));
		when(coach.getId()).thenReturn(UUID.randomUUID());
		when(sessions.findConflictIdExcluding(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
		when(sessions.findRoomImpactsForUpdate(sourceId, now.plusSeconds(3600), now.plusSeconds(7200), false, now))
			.thenReturn(java.util.List.of(session));
		when(rooms.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
		when(rooms.findByIdForUpdate(targetId)).thenReturn(Optional.of(target));
		when(suspensions.existsOverlap(sourceId, now.plusSeconds(3600), now.plusSeconds(7200))).thenReturn(false);
		when(suspensions.saveAndFlush(any(RoomSuspension.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.applyImpact(managerId, sourceId, new RoomImpactPreviewRequest(
			now.plusSeconds(3600), now.plusSeconds(7200), false, "Repair", java.util.List.of(
				new RoomSessionResolutionRequest(sessionId, " MOVE ", targetId, null, null, "Move"))));

		InOrder ordered = inOrder(rooms);
		ordered.verify(rooms).findByIdForUpdate(targetId);
		ordered.verify(rooms).findByIdForUpdate(sourceId);
		verify(sessionService).moveForRoomImpact(managerId, session, target, "Move");
	}

	@Test
	void rejectsRescheduleInsidePendingSuspensionBeforeApplyingResolution() {
		UUID managerId = activeManager();
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		ClassSession session = org.mockito.Mockito.mock(ClassSession.class);
		UUID sessionId = UUID.randomUUID();
		when(session.getId()).thenReturn(sessionId);
		when(session.getStatus()).thenReturn(ClassSessionStatus.SCHEDULED);
		when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(sessions.findRoomImpactsForUpdate(room.getId(), now.plusSeconds(3600), now.plusSeconds(7200), false, now))
			.thenReturn(java.util.List.of(session));

		assertThatThrownBy(() -> service.applyImpact(managerId, room.getId(), new RoomImpactPreviewRequest(
			now.plusSeconds(3600), now.plusSeconds(7200), false, "Repair", java.util.List.of(
				new RoomSessionResolutionRequest(sessionId, "RESCHEDULE", null,
					now.plusSeconds(5400), now.plusSeconds(9000), "Move later")))))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_SUSPENSION_CONFLICT");
		verify(sessionService, never()).rescheduleForRoomImpact(any(), any(), any(), any(), any());
	}

	@Test
	void rejectsPermanentDisableWithInProgressSessionBeforeApplyingResolutions() {
		UUID managerId = activeManager();
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		ClassSession session = org.mockito.Mockito.mock(ClassSession.class);
		UUID sessionId = UUID.randomUUID();
		when(session.getId()).thenReturn(sessionId);
		when(session.getStatus()).thenReturn(ClassSessionStatus.IN_PROGRESS);
		when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(sessions.findRoomImpactsForUpdate(room.getId(), now, now, true, now)).thenReturn(java.util.List.of(session));

		assertThatThrownBy(() -> service.applyImpact(managerId, room.getId(), new RoomImpactPreviewRequest(
			null, null, true, "Retire", java.util.List.of(
				new RoomSessionResolutionRequest(sessionId, "CANCEL", null, null, null, "Cancel")))))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_HAS_IN_PROGRESS_SESSION");
		verify(cancellationService, never()).cancel(any(), any(), any());
		verify(roomService, never()).disableAfterImpactResolution(any(), any(), any());
	}

	@Test
	void rejectsMoveToSourceRoomBeforeTakingAnyRoomLock() {
		UUID managerId = activeManager();
		UUID roomId = UUID.randomUUID();

		assertThatThrownBy(() -> service.applyImpact(managerId, roomId, new RoomImpactPreviewRequest(
			now.plusSeconds(3600), now.plusSeconds(7200), false, "Repair", java.util.List.of(
				new RoomSessionResolutionRequest(UUID.randomUUID(), " move ", roomId, null, null, "Move")))))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("VALIDATION_ERROR");
		org.mockito.Mockito.verifyNoInteractions(rooms);
	}

	@Test
	void rejectsMoveToInactiveRoomAsRoomConflict() {
		UUID managerId = activeManager();
		MoveFixture fixture = moveFixture(RoomStatus.INACTIVE, 20);

		assertThatThrownBy(() -> service.applyImpact(managerId, fixture.source.getId(), fixture.request()))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_INACTIVE");
		verify(sessionService, never()).moveForRoomImpact(any(), any(), any(), any());
	}

	@Test
	void rejectsMoveWhenTargetCapacityIsInsufficientWithConflictMetadata() {
		UUID managerId = activeManager();
		MoveFixture fixture = moveFixture(RoomStatus.ACTIVE, 5);

		assertThatThrownBy(() -> service.applyImpact(managerId, fixture.source.getId(), fixture.request()))
			.isInstanceOf(RoomException.class)
			.satisfies(error -> {
				RoomException exception = (RoomException) error;
				assertThat(exception.getCode()).isEqualTo("ROOM_CAPACITY_CONFLICT");
				assertThat(exception.getSessionId()).isEqualTo(fixture.sessionId);
				assertThat(exception.getTargetRoomId()).isEqualTo(fixture.target.getId());
			});
	}

	@Test
	void rejectsMoveIntoSuspendedTargetRoom() {
		UUID managerId = activeManager();
		MoveFixture fixture = moveFixture(RoomStatus.ACTIVE, 20);
		when(rooms.existsSuspensionOverlap(fixture.target.getId(), fixture.start, fixture.end)).thenReturn(true);

		assertThatThrownBy(() -> service.applyImpact(managerId, fixture.source.getId(), fixture.request()))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_SUSPENSION_CONFLICT");
	}

	@Test
	void rejectsMoveWithConflictingSessionId() {
		UUID managerId = activeManager();
		MoveFixture fixture = moveFixture(RoomStatus.ACTIVE, 20);
		UUID conflictId = UUID.randomUUID();
		when(sessions.findConflictIdExcluding(fixture.sessionId, fixture.coach.getId(), fixture.target.getId(),
			fixture.start, fixture.end)).thenReturn(Optional.of(conflictId));

		assertThatThrownBy(() -> service.applyImpact(managerId, fixture.source.getId(), fixture.request()))
			.isInstanceOf(RoomException.class)
			.satisfies(error -> {
				RoomException exception = (RoomException) error;
				assertThat(exception.getCode()).isEqualTo("ROOM_SESSION_CONFLICT");
				assertThat(exception.getConflictingSessionId()).isEqualTo(conflictId);
			});
	}

	@Test
	void validRescheduleOutsidePendingSuspensionApplies() {
		UUID managerId = activeManager();
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		ClassSession session = org.mockito.Mockito.mock(ClassSession.class);
		UUID sessionId = UUID.randomUUID();
		Instant suspensionStart = now.plusSeconds(3600);
		Instant suspensionEnd = now.plusSeconds(7200);
		Instant newStart = now.plusSeconds(9000);
		Instant newEnd = now.plusSeconds(10800);
		when(session.getId()).thenReturn(sessionId);
		when(session.getStatus()).thenReturn(ClassSessionStatus.SCHEDULED);
		when(session.getRoom()).thenReturn(room);
		when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(sessions.findRoomImpactsForUpdate(room.getId(), suspensionStart, suspensionEnd, false, now))
			.thenReturn(java.util.List.of(session));
		when(rooms.existsSuspensionOverlap(room.getId(), newStart, newEnd)).thenReturn(false);
		when(suspensions.existsOverlap(room.getId(), suspensionStart, suspensionEnd)).thenReturn(false);
		when(suspensions.saveAndFlush(any(RoomSuspension.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.applyImpact(managerId, room.getId(), new RoomImpactPreviewRequest(
			suspensionStart, suspensionEnd, false, "Repair", java.util.List.of(
				new RoomSessionResolutionRequest(sessionId, " RESCHEDULE ", null, newStart, newEnd, "Move later"))));

		verify(sessionService).rescheduleForRoomImpact(managerId, session, newStart, newEnd, "Move later");
	}

	@Test
	void oppositeDirectionMovesRendezvousBeforeRoomLocksAndCompleteWithinTimeout() throws Exception {
		UUID managerId = activeManager();
		UUID roomAId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID roomBId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Room roomA = new Room(roomAId, "A", 20, RoomStatus.ACTIVE);
		Room roomB = new Room(roomBId, "B", 20, RoomStatus.ACTIVE);
		MoveFixture first = concurrentMoveFixture(roomA, roomB);
		MoveFixture second = concurrentMoveFixture(roomB, roomA);
		CyclicBarrier rendezvous = new CyclicBarrier(2);
		org.mockito.Mockito.doAnswer(invocation -> {
			rendezvous.await(5, TimeUnit.SECONDS);
			return null;
		}).when(lockRendezvous).await();
		List<LockCall> lockCalls = Collections.synchronizedList(new ArrayList<>());
		when(rooms.findByIdForUpdate(any())).thenAnswer(invocation -> {
			UUID roomId = invocation.getArgument(0);
			lockCalls.add(new LockCall(Thread.currentThread().getName(), roomId));
			return Optional.of(roomId.equals(roomAId) ? roomA : roomB);
		});
		when(sessions.findRoomImpactsForUpdate(eq(roomAId), any(), any(), eq(false), eq(now)))
			.thenReturn(java.util.List.of(first.session));
		when(sessions.findRoomImpactsForUpdate(eq(roomBId), any(), any(), eq(false), eq(now)))
			.thenReturn(java.util.List.of(second.session));
		when(suspensions.existsOverlap(any(), any(), any())).thenReturn(false);
		when(suspensions.saveAndFlush(any(RoomSuspension.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(sessions.findConflictIdExcluding(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<RoomResponse> firstResult = executor.submit(() -> service.applyImpact(managerId, roomAId, first.request));
			Future<RoomResponse> secondResult = executor.submit(() -> service.applyImpact(managerId, roomBId, second.request));
			assertThat(firstResult.get(5, TimeUnit.SECONDS).status()).isEqualTo(RoomStatus.ACTIVE);
			assertThat(secondResult.get(5, TimeUnit.SECONDS).status()).isEqualTo(RoomStatus.ACTIVE);
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(lockCalls).hasSize(4);
		assertThat(lockCalls.stream().map(LockCall::threadName).distinct()).hasSize(2);
		for (String threadName : lockCalls.stream().map(LockCall::threadName).distinct().toList()) {
			assertThat(lockCalls.stream().filter(call -> call.threadName().equals(threadName)).map(LockCall::roomId).toList())
				.containsExactly(roomAId, roomBId);
		}
	}

	private MoveFixture concurrentMoveFixture(Room source, Room target) {
		ClassSession session = org.mockito.Mockito.mock(ClassSession.class);
		com.scms.backend.account.Account coach = org.mockito.Mockito.mock(com.scms.backend.account.Account.class);
		UUID sessionId = UUID.randomUUID();
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(session.getId()).thenReturn(sessionId);
		when(session.getStatus()).thenReturn(ClassSessionStatus.SCHEDULED);
		when(session.getCapacity()).thenReturn(10);
		when(session.getTeachingCoach()).thenReturn(coach);
		when(session.getStartTime()).thenReturn(start);
		when(session.getEndTime()).thenReturn(end);
		when(coach.getId()).thenReturn(UUID.randomUUID());
		return new MoveFixture(source, target, sessionId, coach, start, end,
			new RoomImpactPreviewRequest(start, end, false, "Repair", java.util.List.of(
				new RoomSessionResolutionRequest(sessionId, "MOVE", target.getId(), null, null, "Move"))), session);
	}

	private MoveFixture moveFixture(RoomStatus targetStatus, int targetCapacity) {
		Room source = new Room(UUID.randomUUID(), "Source", 20, RoomStatus.ACTIVE);
		Room target = new Room(UUID.randomUUID(), "Target", targetCapacity, targetStatus);
		ClassSession session = org.mockito.Mockito.mock(ClassSession.class);
		com.scms.backend.account.Account coach = org.mockito.Mockito.mock(com.scms.backend.account.Account.class);
		UUID sessionId = UUID.randomUUID();
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(session.getId()).thenReturn(sessionId);
		lenient().when(session.getStatus()).thenReturn(ClassSessionStatus.SCHEDULED);
		lenient().when(session.getCapacity()).thenReturn(10);
		lenient().when(session.getTeachingCoach()).thenReturn(coach);
		lenient().when(session.getStartTime()).thenReturn(start);
		lenient().when(session.getEndTime()).thenReturn(end);
		lenient().when(coach.getId()).thenReturn(UUID.randomUUID());
		lenient().when(sessions.findConflictIdExcluding(sessionId, coach.getId(), target.getId(), start, end))
			.thenReturn(Optional.empty());
		when(sessions.findRoomImpactsForUpdate(source.getId(), start, end, false, now))
			.thenReturn(java.util.List.of(session));
		when(rooms.findByIdForUpdate(source.getId())).thenReturn(Optional.of(source));
		when(rooms.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
		return new MoveFixture(source, target, sessionId, coach, start, end,
			new RoomImpactPreviewRequest(start, end, false, "Repair", java.util.List.of(
				new RoomSessionResolutionRequest(sessionId, "MOVE", target.getId(), null, null, "Move"))), session);
	}

	private record MoveFixture(Room source, Room target, UUID sessionId,
		com.scms.backend.account.Account coach, Instant start, Instant end,
		RoomImpactPreviewRequest request, ClassSession session) { }

	private record LockCall(String threadName, UUID roomId) { }

	private UUID activeManager() {
		UUID id = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		return id;
	}
}
