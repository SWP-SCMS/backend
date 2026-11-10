package com.scms.backend.course.run;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public record CourseWeeklyRunCreateRequest(@NotBlank @Size(max = 50) String runCode, @NotNull UUID courseId, @NotNull UUID primaryCoachId, @NotNull UUID roomId, @NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger tuitionVnd, @Positive int capacity, @Positive int minimumEnrollment, @NotNull Instant registrationOpensAt, @NotNull Instant registrationClosesAt, boolean namedCoachCommitment, @NotNull @Valid CourseWeeklyScheduleRequest schedule) {
}
