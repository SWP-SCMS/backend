package com.scms.backend.course.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

public record CourseSessionPatchRequest(@NotNull @PositiveOrZero Long expectedVersion, UUID roomId, UUID coachId, @NotNull Instant startAt, @NotNull Instant endAt) {
}
