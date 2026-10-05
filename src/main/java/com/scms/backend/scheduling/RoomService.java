package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

	private final RoomRepository rooms;
	private final AccountRepository accounts;

	RoomService(RoomRepository rooms, AccountRepository accounts) {
		this.rooms = rooms;
		this.accounts = accounts;
	}

	@Transactional(readOnly = true)
	List<RoomResponse> list(UUID managerId) {
		ensureActiveManager(managerId);
		Sort sort = Sort.by("name").ascending().and(Sort.by("id").ascending());
		return rooms.findAll(sort).stream().map(RoomResponse::from).toList();
	}

	@Transactional(readOnly = true)
	RoomResponse get(UUID managerId, UUID roomId) {
		ensureActiveManager(managerId);
		return RoomResponse.from(findRoom(roomId));
	}

	@Transactional
	RoomResponse create(UUID managerId, RoomCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw RoomException.validation("request", "must be a JSON object");
		String name = normalizeName(request.name());
		int capacity = validateCapacity(request.capacity());
		if (rooms.existsByName(name)) throw RoomException.duplicateName();
		Room room = new Room(UUID.randomUUID(), name, capacity, RoomStatus.ACTIVE);
		flush(room);
		return RoomResponse.from(room);
	}

	@Transactional
	RoomResponse update(UUID managerId, UUID roomId, RoomPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || (request.name() == null && request.capacity() == null && request.status() == null)) {
			throw RoomException.validation("request", "at least one field is required");
		}
		Room room = findRoom(roomId);
		String name = request.name() == null ? room.getName() : normalizeName(request.name());
		if (!name.equals(room.getName()) && rooms.existsByNameAndIdNot(name, roomId)) {
			throw RoomException.duplicateName();
		}
		int capacity = request.capacity() == null ? room.getCapacity() : validateCapacity(request.capacity());
		if (request.capacity() != null && rooms.existsActiveSessionOverCapacity(roomId, capacity)) {
			throw RoomException.capacityConflict();
		}
		RoomStatus status = request.status() == null ? room.getStatus() : request.status();
		room.update(name, capacity, status);
		flush(room);
		return RoomResponse.from(room);
	}

	private Room findRoom(UUID roomId) {
		return rooms.findById(roomId).orElseThrow(RoomException::notFound);
	}

	private void flush(Room room) {
		try {
			rooms.saveAndFlush(room);
		}
		catch (DataIntegrityViolationException exception) {
			throw RoomException.duplicateName();
		}
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String normalizeName(String value) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > 150) {
			throw RoomException.validation("name", "must be non-blank and at most 150 characters");
		}
		return value.trim();
	}

	private int validateCapacity(Integer value) {
		if (value == null || value <= 0) {
			throw RoomException.validation("capacity", "must be greater than zero");
		}
		return value;
	}
}
