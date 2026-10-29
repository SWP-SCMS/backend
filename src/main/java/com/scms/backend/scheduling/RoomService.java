package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.CourseSessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

	private final RoomRepository rooms;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final CourseSessionRepository courseSessions;

	RoomService(RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits) {
		this(rooms, accounts, audits, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	RoomService(RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits,
			CourseSessionRepository courseSessions) {
		this.rooms = rooms;
		this.accounts = accounts;
		this.audits = audits;
		this.courseSessions = courseSessions;
	}

	@Transactional(readOnly = true)
	List<RoomResponse> list(UUID managerId) {
		ensureActiveManager(managerId);
		Sort sort = Sort.by("name").ascending().and(Sort.by("id").ascending());
		return rooms.findAll(sort).stream().map(RoomResponse::from).toList();
	}

	@Transactional(readOnly = true)
	List<RoomAvailabilityResponse> availability(UUID managerId, Instant startTime, Instant endTime,
			Integer requestedCapacity, UUID excludeSessionId) {
		ensureActiveManager(managerId);
		if (startTime == null) throw RoomException.validation("startTime", "is required");
		if (endTime == null || !endTime.isAfter(startTime)) {
			throw RoomException.validation("endTime", "must be later than startTime");
		}
		if (requestedCapacity == null || requestedCapacity <= 0) {
			throw RoomException.validation("requestedCapacity", "must be greater than zero");
		}
		return rooms.findAvailability(startTime, endTime, excludeSessionId).stream()
			.map(room -> availabilityResponse(room, requestedCapacity))
			.toList();
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
		RoomUsageMode usageMode = request.usageMode() == null ? RoomUsageMode.EXCLUSIVE : request.usageMode();
		Room room = new Room(UUID.randomUUID(), name, capacity, RoomStatus.ACTIVE, usageMode);
		flush(room);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_CREATED", "ROOM", room.getId(),
			snapshot(room)));
		return RoomResponse.from(room);
	}

	@Transactional
	RoomResponse update(UUID managerId, UUID roomId, RoomPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || (request.name() == null && request.capacity() == null && request.status() == null
				&& request.usageMode() == null)) {
			throw RoomException.validation("request", "at least one field is required");
		}
		Room room = findRoomForUpdate(roomId);
		Map<String, Object> before = snapshot(room);
		String name = request.name() == null ? room.getName() : normalizeName(request.name());
		if (!name.equals(room.getName()) && rooms.existsByNameAndIdNot(name, roomId)) {
			throw RoomException.duplicateName();
		}
		int capacity = request.capacity() == null ? room.getCapacity() : validateCapacity(request.capacity());
		RoomUsageMode usageMode = request.usageMode() == null ? room.getUsageMode() : request.usageMode();
		if (request.capacity() != null && (rooms.existsActiveSessionOverCapacity(roomId, capacity)
				|| (courseSessions != null && courseSessions.existsRunOverRoomCapacity(roomId, capacity))
				|| (usageMode == RoomUsageMode.SHARED && rooms.existsActiveLoadOverCapacity(roomId, capacity)))) {
			throw RoomException.capacityConflict();
		}
		if (usageMode == RoomUsageMode.EXCLUSIVE && room.getUsageMode() != RoomUsageMode.EXCLUSIVE
				&& rooms.existsActiveSessionOverlapPair(roomId)) {
			throw RoomException.capacityConflict();
		}
		RoomStatus status = request.status() == null ? room.getStatus() : request.status();
		if (room.getStatus() != RoomStatus.INACTIVE && status == RoomStatus.INACTIVE) {
			List<UUID> blockingSessionIds = rooms.findBlockingSessionIds(roomId);
			if (courseSessions != null) blockingSessionIds = java.util.stream.Stream.concat(
				blockingSessionIds.stream(), courseSessions.findBlockingRoomSessionIds(roomId).stream()).distinct().toList();
			if (!blockingSessionIds.isEmpty()) throw RoomException.activeSessionsConflict(blockingSessionIds);
		}
		room.update(name, capacity, status, usageMode);
		Map<String, Object> after = snapshot(room);
		if (before.equals(after)) return RoomResponse.from(room);
		flush(room);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_UPDATED", "ROOM", roomId,
			null, before, after));
		return RoomResponse.from(room);
	}

	void disableAfterImpactResolution(UUID managerId, Room room, String reason) {
		ensureActiveManager(managerId);
		if (room.getStatus() == RoomStatus.INACTIVE) return;
		Map<String, Object> before = snapshot(room);
		room.update(room.getName(), room.getCapacity(), RoomStatus.INACTIVE);
		flush(room);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_UPDATED", "ROOM", room.getId(),
			reason, before, snapshot(room)));
	}

	private Map<String, Object> snapshot(Room room) {
		return Map.of("name", room.getName(), "capacity", room.getCapacity(), "status", room.getStatus().name(),
			"usageMode", room.getUsageMode().name());
	}

	private RoomAvailabilityResponse availabilityResponse(RoomAvailabilityView room, int requestedCapacity) {
		return availabilityResponse(room.getRoomId(), room.getName(), room.getUsageMode(), room.getCapacity(),
			room.getOccupiedCapacity(), room.getSuspended(), requestedCapacity);
	}

	RoomAvailabilityResponse availabilityResponse(RoomSessionAvailabilityView room, int requestedCapacity) {
		return availabilityResponse(room.getRoomId(), room.getName(), room.getUsageMode(), room.getCapacity(),
			room.getOccupiedCapacity(), room.getSuspended(), requestedCapacity);
	}

	private RoomAvailabilityResponse availabilityResponse(UUID roomId, String name, String usageModeValue,
			int capacity, long occupiedCapacity, boolean suspended, int requestedCapacity) {
		RoomUsageMode usageMode = RoomUsageMode.valueOf(usageModeValue);
		long availableCapacity = suspended || usageMode == RoomUsageMode.EXCLUSIVE && occupiedCapacity > 0
			? 0 : Math.max(0, (long) capacity - occupiedCapacity);
		boolean available = !suspended && requestedCapacity <= capacity
			&& (usageMode == RoomUsageMode.EXCLUSIVE
			? occupiedCapacity == 0 : occupiedCapacity + requestedCapacity <= capacity);
		return new RoomAvailabilityResponse(roomId, name, usageMode, capacity, occupiedCapacity,
			availableCapacity, available);
	}

	private Room findRoom(UUID roomId) {
		return rooms.findById(roomId).orElseThrow(RoomException::notFound);
	}

	private Room findRoomForUpdate(UUID roomId) {
		Optional<Room> result = rooms.findByIdForUpdate(roomId);
		if (result == null || result.isEmpty()) result = rooms.findById(roomId);
		return result.orElseThrow(RoomException::notFound);
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
