package com.scms.backend.course.session;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record CourseSessionCreateRequest(@Positive int unitNumber, @NotNull UUID roomId, @NotNull UUID coachId, @NotNull Instant startAt, @NotNull Instant endAt, @NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger allocationVnd) {
}
