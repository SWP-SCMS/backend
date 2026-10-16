package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomSuspensionService {

	private final RoomSuspensionRepository suspensions;
	private final RoomRepository rooms;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;

	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts,
			AuditEventRepository audits, Clock clock) {
		this.suspensions = suspensions;
		this.rooms = rooms;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	List<RoomSuspensionResponse> list(UUID managerId, UUID roomId) {
		ensureActiveManager(managerId);
		findRoom(roomId);
		return suspensions.findByRoom_IdOrderByStartTimeAsc(roomId).stream()
			.map(RoomSuspensionResponse::from).toList();
	}

	@Transactional
	RoomSuspensionResponse create(UUID managerId, UUID roomId, RoomSuspensionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw RoomException.suspensionValidation("request", "must be a JSON object");
		Room room = rooms.findByIdForUpdate(roomId).orElseThrow(RoomException::notFound);
		Instant now = clock.instant();
		if (request.startTime() == null || !request.startTime().isAfter(now)) {
			throw RoomException.suspensionValidation("startTime", "must be in the future");
		}
		if (request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
			throw RoomException.suspensionValidation("endTime", "must be later than startTime");
		}
		String reason = request.reason() == null || request.reason().trim().isEmpty()
			? null : request.reason().trim();
		if (reason != null && reason.length() > 1000) {
			throw RoomException.suspensionValidation("reason", "must be at most 1000 characters");
		}
		if (suspensions.existsOverlap(roomId, request.startTime(), request.endTime())
				|| rooms.existsActiveSessionOverlap(roomId, request.startTime(), request.endTime())) {
			throw RoomException.suspensionConflict();
		}
		RoomSuspension suspension = new RoomSuspension(UUID.randomUUID(), room, request.startTime(),
			request.endTime(), reason, managerId);
		try {
			suspensions.saveAndFlush(suspension);
		}
		catch (DataIntegrityViolationException exception) {
			throw RoomException.suspensionConflict();
		}
		Map<String, Object> after = new HashMap<>();
		after.put("roomId", roomId.toString());
		after.put("startTime", request.startTime().toString());
		after.put("endTime", request.endTime().toString());
		after.put("reason", reason);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_SUSPENDED", "ROOM_SUSPENSION",
			suspension.getId(), reason, null, after));
		return RoomSuspensionResponse.from(suspension);
	}

	private Room findRoom(UUID roomId) {
		return rooms.findById(roomId).orElseThrow(RoomException::notFound);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}
}
