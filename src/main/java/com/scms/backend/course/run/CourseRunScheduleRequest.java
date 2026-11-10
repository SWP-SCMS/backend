package com.scms.backend.course.run;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public record CourseRunScheduleRequest(@NotNull @PositiveOrZero Long expectedVersion, @Schema(description = "Must not precede registration closing. Retained effective sessions must fit within the new Run window.") @NotNull Instant plannedStartAt, @NotNull Instant plannedEndAt, @Schema(description = "Cannot increase after opening. A reduction may only remove units that have not started.") @NotNull @Positive Integer totalUnits) {
}
