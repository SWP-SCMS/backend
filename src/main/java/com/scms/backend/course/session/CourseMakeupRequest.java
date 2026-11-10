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

public record CourseMakeupRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long expectedVersion, @jakarta.validation.constraints.NotNull UUID roomId, @jakarta.validation.constraints.NotNull UUID coachId, @jakarta.validation.constraints.NotNull Instant startAt, @jakarta.validation.constraints.NotNull Instant endAt) {
}
