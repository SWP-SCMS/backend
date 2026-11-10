package com.scms.backend.course.catalog;

import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CatalogCourseRunDetailResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String runCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID primaryCoachId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String primaryCoachName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9999999999999999999") BigInteger tuitionVnd, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int capacity, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationOpensAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationClosesAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus registrationStatus, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedStartAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedEndAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int totalUnits, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "OPEN_FOR_REGISTRATION") CourseRunStatus status, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CatalogCourseSessionResponse> sessions) {

	CatalogCourseRunDetailResponse(CatalogCourseRunResponse run, List<CatalogCourseSessionResponse> sessions) {
		this(run.id(), run.runCode(), run.courseId(), run.courseCode(), run.courseName(), run.primaryCoachId(), run.primaryCoachName(), run.tuitionVnd(), run.capacity(), run.registrationOpensAt(), run.registrationClosesAt(), run.registrationStatus(), run.plannedStartAt(), run.plannedEndAt(), run.totalUnits(), run.status(), sessions);
	}
}
