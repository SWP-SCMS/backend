package com.scms.backend.scheduling;

import java.util.UUID;

public record ClassSessionAssignmentRequest(UUID coachId, UUID roomId) {
}
