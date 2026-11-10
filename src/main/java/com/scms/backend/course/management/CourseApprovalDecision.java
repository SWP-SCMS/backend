package com.scms.backend.course.management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

record CourseApprovalDecision(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max = 1000) String reason) {
}
