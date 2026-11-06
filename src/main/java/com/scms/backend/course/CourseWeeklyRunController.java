package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

record CourseWeeklyRunCreateRequest(
    @NotBlank @Size(max = 50) String runCode,
    @NotNull UUID courseId, @NotNull UUID primaryCoachId, @NotNull UUID roomId,
    @NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger tuitionVnd,
    @Positive int capacity, @Positive int minimumEnrollment,
    @NotNull Instant registrationOpensAt, @NotNull Instant registrationClosesAt,
    boolean namedCoachCommitment,
    @NotNull @Valid CourseWeeklyScheduleRequest schedule) {}

record CourseWeeklyRunResponse(CourseRunResponse run, List<CourseSessionResponse> sessions) {}

@RestController
@RequestMapping("/manager/course-runs")
class CourseWeeklyRunController {
    private final CourseWeeklyRunService service;
    CourseWeeklyRunController(CourseWeeklyRunService service) { this.service = service; }

    @Operation(description = "Preview calendar only, without saving or reserving resources. ISO weekdays: 1=Monday, 7=Sunday. Opening date must match a selected weekday. Times use Asia/Ho_Chi_Minh. End date is calculated from totalUnits.")
    @PostMapping("/preview-schedule")
    CourseWeeklyPreview preview(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CourseWeeklyScheduleRequest request) {
        return service.preview(UUID.fromString(jwt.getSubject()), request);
    }

    @Operation(description = "Atomically create a draft Run and every weekly session. Calculates plannedStartAt/plannedEndAt; does not accept a manually supplied end date. Conflicts roll back the entire operation. Registration remains closed until the existing open-registration action. Retrying a saved runCode returns the existing duplicate-code conflict, never creates another Run.")
    @PostMapping("/with-schedule")
    ResponseEntity<CourseWeeklyRunResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CourseWeeklyRunCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(UUID.fromString(jwt.getSubject()), request));
    }
}
