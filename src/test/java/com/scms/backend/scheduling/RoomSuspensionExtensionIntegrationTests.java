package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class RoomSuspensionExtensionIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired RoomSuspensionService service;
	@Autowired ClassSessionService sessionService;
	@Autowired RoomSuspensionRepository suspensions;
	@Autowired RoomRepository rooms;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired ClassSessionRepository sessions;
	@MockitoBean AuditEventRepository audits;
	@MockitoBean RoomImpactLockRendezvous lockRendezvous;

	@Test
	void assignmentRacingExtensionCompletesWithoutDeadlockAndLeavesValidState() throws Exception {
		Account manager = saveAccount(AccountRole.MANAGER, "Assignment Extension Race Manager");
		Account coach = saveAccount(AccountRole.COACH, "Assignment Extension Race Coach");
		Account replacementCoach = saveAccount(AccountRole.COACH, "Assignment Extension Replacement Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Assignment Extension Race Discipline", null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Assignment Extension Race Class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room source = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Assignment Extension Race Source", 20,
			RoomStatus.ACTIVE));
		Room extensionTarget = rooms.saveAndFlush(new Room(UUID.randomUUID(),
			"Assignment Extension Resolution", 20, RoomStatus.ACTIVE));
		Instant suspensionStart = Instant.parse("2029-12-01T10:00:00Z");
		Instant expectedEndTime = Instant.parse("2030-01-01T10:00:00Z");
		Instant extendedEndTime = Instant.parse("2030-02-01T10:00:00Z");
		RoomSuspension suspension = suspensions.saveAndFlush(new RoomSuspension(UUID.randomUUID(), source,
			suspensionStart, expectedEndTime, "Repair", manager.getId()));
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			source, Instant.parse("2030-01-02T10:00:00Z"), Instant.parse("2030-01-02T11:00:00Z"), 10,
			manager.getId()));
		CyclicBarrier lockBoundary = new CyclicBarrier(2);
		doAnswer(invocation -> {
			lockBoundary.await(5, TimeUnit.SECONDS);
			return null;
		}).when(lockRendezvous).await();

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<Object> extension = executor.submit(() -> outcome(() -> service.extend(manager.getId(),
				source.getId(), suspension.getId(), new RoomSuspensionExtensionRequest(expectedEndTime,
					extendedEndTime, "Extended repair", List.of(new RoomSessionResolutionRequest(session.getId(),
						"MOVE", extensionTarget.getId(), null, null, "Move for extension"))))));
			Future<Object> assignment = executor.submit(() -> outcome(() -> sessionService.updateAssignment(
				manager.getId(), session.getId(), new ClassSessionAssignmentRequest(replacementCoach.getId(), null))));

			Object extensionResult = extension.get(10, TimeUnit.SECONDS);
			Object assignmentResult = assignment.get(10, TimeUnit.SECONDS);
			boolean extensionSucceeded = extensionResult instanceof RoomSuspensionResponse;
			boolean assignmentSucceeded = assignmentResult instanceof ClassSessionDetailResponse;
			assertThat(extensionSucceeded).isTrue();
			assertThat(assignmentSucceeded).isTrue();

			UUID finalRoomId = sessions.findById(session.getId()).orElseThrow().getRoom().getId();
			Instant finalEndTime = suspensions.findById(suspension.getId()).orElseThrow().getEndTime();
			assertThat(finalRoomId).isEqualTo(extensionTarget.getId());
			assertThat(finalEndTime).isEqualTo(extendedEndTime);
		}
	}

	@Test
	void concurrentExtensionsWithSameExpectedEndTimeYieldOneSuccessAndOneStaleConflict() throws Exception {
		Account manager = saveAccount(AccountRole.MANAGER, "Concurrent Extension Manager");
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Concurrent Extension Room", 20,
			RoomStatus.ACTIVE));
		Instant startTime = Instant.parse("2029-12-01T10:00:00Z");
		Instant expectedEndTime = Instant.parse("2030-01-01T10:00:00Z");
		RoomSuspension suspension = suspensions.saveAndFlush(new RoomSuspension(UUID.randomUUID(), room,
			startTime, expectedEndTime, "Repair", manager.getId()));
		CyclicBarrier start = new CyclicBarrier(2);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Object> first = executor.submit(() -> extendAfterBarrier(start, manager.getId(), room.getId(),
				suspension.getId(), expectedEndTime, Instant.parse("2030-02-01T10:00:00Z"), "First"));
			Future<Object> second = executor.submit(() -> extendAfterBarrier(start, manager.getId(), room.getId(),
				suspension.getId(), expectedEndTime, Instant.parse("2030-03-01T10:00:00Z"), "Second"));
			List<Object> results = List.of(first.get(), second.get());

			assertThat(results).filteredOn(RoomSuspensionResponse.class::isInstance).hasSize(1);
			assertThat(results).filteredOn(RoomException.class::isInstance).singleElement().satisfies(result ->
				assertThat(((RoomException) result).getCode()).isEqualTo("ROOM_SUSPENSION_CHANGED"));
			RoomSuspensionResponse winner = (RoomSuspensionResponse) results.stream()
				.filter(RoomSuspensionResponse.class::isInstance).findFirst().orElseThrow();
			assertThat(suspensions.findById(suspension.getId()).orElseThrow().getEndTime())
				.isEqualTo(winner.endTime());
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void lateExtensionFailureRollsBackResolvedSessionAndSuspension() {
		Account manager = saveAccount(AccountRole.MANAGER, "Rollback Extension Manager");
		Account coach = saveAccount(AccountRole.COACH, "Rollback Extension Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Rollback Extension Discipline", null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Rollback Extension Class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room source = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Rollback Extension Source", 20,
			RoomStatus.ACTIVE));
		Room target = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Rollback Extension Target", 20,
			RoomStatus.ACTIVE));
		Instant startTime = Instant.parse("2029-12-01T10:00:00Z");
		Instant expectedEndTime = Instant.parse("2030-01-01T10:00:00Z");
		Instant endTime = Instant.parse("2030-02-01T10:00:00Z");
		RoomSuspension suspension = suspensions.saveAndFlush(new RoomSuspension(UUID.randomUUID(), source,
			startTime, expectedEndTime, "Repair", manager.getId()));
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			source, Instant.parse("2030-01-02T10:00:00Z"), Instant.parse("2030-01-02T11:00:00Z"), 10,
			manager.getId()));
		when(audits.save(any())).thenAnswer(invocation -> {
			com.scms.backend.audit.AuditEvent event = invocation.getArgument(0);
			if ("ROOM_SUSPENSION_EXTENDED".equals(event.getAction())) {
				throw new IllegalStateException("audit failed");
			}
			return event;
		});

		assertThatThrownBy(() -> service.extend(manager.getId(), source.getId(), suspension.getId(),
			new RoomSuspensionExtensionRequest(expectedEndTime, endTime, "Extended repair", List.of(
				new RoomSessionResolutionRequest(session.getId(), "MOVE", target.getId(), null, null, "Move")))))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("audit failed");

		assertThat(suspensions.findById(suspension.getId()).orElseThrow().getEndTime()).isEqualTo(expectedEndTime);
		assertThat(sessions.findById(session.getId()).orElseThrow().getRoom().getId()).isEqualTo(source.getId());
	}

	private Object extendAfterBarrier(CyclicBarrier start, UUID managerId, UUID roomId, UUID suspensionId,
			Instant expectedEndTime, Instant endTime, String reason) {
		try {
			start.await();
			return service.extend(managerId, roomId, suspensionId,
				new RoomSuspensionExtensionRequest(expectedEndTime, endTime, reason, List.of()));
		}
		catch (RoomException exception) {
			return exception;
		}
		catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	private Object outcome(Supplier<Object> action) {
		try {
			return action.get();
		}
		catch (RuntimeException exception) {
			return exception;
		}
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
