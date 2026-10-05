package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class RoomServiceTests {

	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	@InjectMocks RoomService service;

	@Test
	void activeManagerCreatesNormalizedActiveRoom() {
		UUID managerId = activeManager();
		when(rooms.existsByName("Studio A")).thenReturn(false);
		when(rooms.saveAndFlush(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RoomResponse result = service.create(managerId, new RoomCreateRequest("  Studio A  ", 20));

		assertThat(result.name()).isEqualTo("Studio A");
		assertThat(result.capacity()).isEqualTo(20);
		assertThat(result.status()).isEqualTo(RoomStatus.ACTIVE);
	}

	@Test
	void createRejectsNonPositiveCapacity() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.create(managerId, new RoomCreateRequest("Studio A", 0)))
			.isInstanceOf(RoomException.class)
			.extracting("field").isEqualTo("capacity");
	}

	@Test
	void createRejectsDuplicateName() {
		UUID managerId = activeManager();
		when(rooms.existsByName("Studio A")).thenReturn(true);

		assertThatThrownBy(() -> service.create(managerId, new RoomCreateRequest("Studio A", 20)))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_NAME_CONFLICT");
	}

	@Test
	void inactiveManagerCannotListRooms() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.list(managerId))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	@Test
	void listUsesStableNameThenIdOrdering() {
		UUID managerId = activeManager();
		when(rooms.findAll(any(Sort.class))).thenReturn(List.of(room(20)));

		assertThat(service.list(managerId)).singleElement().extracting(RoomResponse::name)
			.isEqualTo("Studio A");
		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(rooms).findAll(sort.capture());
		assertThat(sort.getValue().getOrderFor("name")).isNotNull();
		assertThat(sort.getValue().getOrderFor("id")).isNotNull();
	}

	@Test
	void getReturnsNotFoundForUnknownRoom() {
		UUID managerId = activeManager();
		UUID roomId = UUID.randomUUID();
		when(rooms.findById(roomId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(managerId, roomId))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_NOT_FOUND");
	}

	@Test
	void patchRejectsCapacityBelowScheduledOrInProgressSession() {
		UUID managerId = activeManager();
		Room room = room(30);
		when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
		when(rooms.existsActiveSessionOverCapacity(room.getId(), 10)).thenReturn(true);

		assertThatThrownBy(() -> service.update(managerId, room.getId(),
			new RoomPatchRequest(null, 10, null)))
			.isInstanceOf(RoomException.class)
			.extracting("code").isEqualTo("ROOM_CAPACITY_CONFLICT");
	}

	@Test
	void patchUpdatesProvidedFieldsAndCanDeactivate() {
		UUID managerId = activeManager();
		Room room = room(20);
		when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
		when(rooms.existsByNameAndIdNot("Studio B", room.getId())).thenReturn(false);
		when(rooms.existsActiveSessionOverCapacity(room.getId(), 25)).thenReturn(false);

		RoomResponse result = service.update(managerId, room.getId(),
			new RoomPatchRequest("  Studio B  ", 25, RoomStatus.INACTIVE));

		assertThat(result.name()).isEqualTo("Studio B");
		assertThat(result.capacity()).isEqualTo(25);
		assertThat(result.status()).isEqualTo(RoomStatus.INACTIVE);
	}

	@Test
	void patchRejectsEmptyBody() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.update(managerId, UUID.randomUUID(),
			new RoomPatchRequest(null, null, null)))
			.isInstanceOf(RoomException.class)
			.extracting("field").isEqualTo("request");
	}

	private UUID activeManager() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		return managerId;
	}

	private Room room(int capacity) {
		return new Room(UUID.randomUUID(), "Studio A", capacity, RoomStatus.ACTIVE);
	}
}
