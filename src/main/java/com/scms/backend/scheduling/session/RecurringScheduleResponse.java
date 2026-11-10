package com.scms.backend.scheduling.session;

import java.util.List;
import java.util.UUID;

public record RecurringScheduleResponse(UUID id, int sessionCount, List<UUID> sessionIds) {
}
