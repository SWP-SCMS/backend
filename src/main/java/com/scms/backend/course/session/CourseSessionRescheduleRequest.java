package com.scms.backend.course.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

public record CourseSessionRescheduleRequest(@NotNull UUID sessionId, @NotNull @PositiveOrZero Long expectedVersion, @NotNull Instant startAt, @NotNull Instant endAt, UUID roomId, UUID coachId) {
}
