package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
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
	RoomSuspensionService service;

	private final Instant now = Instant.parse("2026-10-05T00:00:00Z");

	@BeforeEach
	void setUp() {
		service = new RoomSuspensionService(suspensions, rooms, accounts, audits,
			Clock.fixed(now, ZoneOffset.UTC));
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

	private UUID activeManager() {
		UUID id = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		return id;
	}
}
