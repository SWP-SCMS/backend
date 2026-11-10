package com.scms.backend.course.management;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.scms.backend.course.CourseFieldLimits;
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

public record CourseCreateRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 200) String name, @Size(max = CourseFieldLimits.DESCRIPTION) @Schema(types = { "string", "null" }) String description, @NotNull UUID disciplineId, @Size(max = CourseFieldLimits.IMAGE_URL) @Schema(types = { "string", "null" }) String imageUrl, @Positive Integer defaultMinimumEnrollment) {
}
