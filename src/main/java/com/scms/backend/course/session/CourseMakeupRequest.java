package com.scms.backend.course.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

public record CourseMakeupRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long expectedVersion, @jakarta.validation.constraints.NotNull UUID roomId, @jakarta.validation.constraints.NotNull UUID coachId, @jakarta.validation.constraints.NotNull Instant startAt, @jakarta.validation.constraints.NotNull Instant endAt) {
}
