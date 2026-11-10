package com.scms.backend.course.run;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CourseRunOpenRequest(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max = 100) String policyVersion) {
}
