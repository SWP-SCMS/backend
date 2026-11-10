package com.scms.backend.course.run;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record CourseWeeklyRunCreateRequest(@NotBlank @Size(max = 50) String runCode, @NotNull UUID courseId, @NotNull UUID primaryCoachId, @NotNull UUID roomId, @NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger tuitionVnd, @Positive int capacity, @Positive int minimumEnrollment, @NotNull Instant registrationOpensAt, @NotNull Instant registrationClosesAt, boolean namedCoachCommitment, @NotNull @Valid CourseWeeklyScheduleRequest schedule) {
}
