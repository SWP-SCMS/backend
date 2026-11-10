package com.scms.backend.course.run;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.scms.backend.course.session.CourseSessionRescheduleRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseRunRescheduleRequest(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Instant plannedStartAt, @NotNull Instant plannedEndAt, @NotNull @Positive Integer totalUnits, @Schema(description = "Final schedule for listed sessions; unlisted sessions retain their schedule and assignment.") @NotNull List<@NotNull @jakarta.validation.Valid CourseSessionRescheduleRequest> sessions) {
}
