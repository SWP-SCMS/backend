package com.scms.backend.scheduling.session;

import java.util.UUID;

public record ClassSessionAssignmentRequest(UUID coachId, UUID roomId, Long expectedVersion) {

	public ClassSessionAssignmentRequest(UUID coachId, UUID roomId) {
		this(coachId, roomId, null);
	}
}
