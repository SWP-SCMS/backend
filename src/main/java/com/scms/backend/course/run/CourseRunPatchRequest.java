package com.scms.backend.course.run;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CourseRunPatchRequest(@NotNull @PositiveOrZero Long expectedVersion, @NotNull @Positive Integer minimumEnrollment) {
}
