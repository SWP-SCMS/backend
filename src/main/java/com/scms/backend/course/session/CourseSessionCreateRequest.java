package com.scms.backend.course.session;

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

public record CourseSessionCreateRequest(@Positive int unitNumber, @NotNull UUID roomId, @NotNull UUID coachId, @NotNull Instant startAt, @NotNull Instant endAt, @NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger allocationVnd) {
}
