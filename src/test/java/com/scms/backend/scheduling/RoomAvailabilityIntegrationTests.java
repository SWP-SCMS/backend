package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class RoomAvailabilityIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired RoomService service;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired RoomSuspensionRepository suspensions;
	@Autowired ClassSessionRepository sessions;

	@Test
	void discoversExclusiveSharedSuspendedAndExcludedSessionAvailability() {
		Account manager = saveAccount(AccountRole.MANAGER, "Availability Manager");
		Account coach = saveAccount(AccountRole.COACH, "Availability Coach");
		Account sharedCoach = saveAccount(AccountRole.COACH, "Availability Shared Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Availability Discipline",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Availability Class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room exclusive = saveRoom("Availability Exclusive", 20, RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE);
		Room shared = saveRoom("Availability Shared", 10, RoomStatus.ACTIVE, RoomUsageMode.SHARED);
		Room suspended = saveRoom("Availability Suspended", 20, RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE);
		saveRoom("Availability Inactive", 20, RoomStatus.INACTIVE, RoomUsageMode.EXCLUSIVE);
		Instant startTime = Instant.parse("2030-01-01T10:00:00Z");
		Instant endTime = Instant.parse("2030-01-01T11:00:00Z");
		ClassSession exclusiveSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null,
			coach, exclusive, startTime, endTime, 8, manager.getId()));
		sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, sharedCoach, shared, startTime,
			endTime, 6, manager.getId()));
		suspensions.saveAndFlush(new RoomSuspension(UUID.randomUUID(), suspended, startTime, endTime,
			"Maintenance", manager.getId()));

		List<RoomAvailabilityResponse> availability = service.availability(manager.getId(), startTime, endTime, 4,
			null);

		assertThat(availability).extracting(RoomAvailabilityResponse::name)
			.containsExactly("Availability Exclusive", "Availability Shared", "Availability Suspended");
		assertThat(find(availability, exclusive.getId()))
			.extracting(RoomAvailabilityResponse::occupiedCapacity, RoomAvailabilityResponse::availableCapacity,
				RoomAvailabilityResponse::available)
			.containsExactly(8L, 0L, false);
		assertThat(find(availability, shared.getId()))
			.extracting(RoomAvailabilityResponse::occupiedCapacity, RoomAvailabilityResponse::availableCapacity,
				RoomAvailabilityResponse::available)
			.containsExactly(6L, 4L, true);
		assertThat(find(availability, suspended.getId()).available()).isFalse();
		assertThat(find(service.availability(manager.getId(), startTime, endTime, 5, null), shared.getId())
			.available()).isFalse();

		RoomAvailabilityResponse excluded = find(service.availability(manager.getId(), startTime, endTime, 20,
			exclusiveSession.getId()), exclusive.getId());
		assertThat(excluded.occupiedCapacity()).isZero();
		assertThat(excluded.available()).isTrue();
	}

	private Room saveRoom(String name, int capacity, RoomStatus status, RoomUsageMode usageMode) {
		return rooms.saveAndFlush(new Room(UUID.randomUUID(), name, capacity, status, usageMode));
	}

	private RoomAvailabilityResponse find(List<RoomAvailabilityResponse> values, UUID roomId) {
		return values.stream().filter(value -> value.roomId().equals(roomId)).findFirst().orElseThrow();
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
