package com.scms.backend.course.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CourseSessionDisruptionRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long expectedVersion) {
}
