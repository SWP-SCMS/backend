package com.scms.backend.course.run;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
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

public record CourseRunScheduleRequest(@NotNull @PositiveOrZero Long expectedVersion, @Schema(description = "Must not precede registration closing. Retained effective sessions must fit within the new Run window.") @NotNull Instant plannedStartAt, @NotNull Instant plannedEndAt, @Schema(description = "Cannot increase after opening. A reduction may only remove units that have not started.") @NotNull @Positive Integer totalUnits) {
}
