package com.scms.backend.course.catalog;

import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

record CatalogCourseRunResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String runCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID primaryCoachId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String primaryCoachName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9999999999999999999") BigInteger tuitionVnd, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int capacity, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationOpensAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationClosesAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus registrationStatus, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedStartAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedEndAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int totalUnits, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "OPEN_FOR_REGISTRATION") CourseRunStatus status) {

	static CatalogCourseRunResponse from(CourseRun run, Instant now) {
		return new CatalogCourseRunResponse(run.getId(), run.getRunCode(), run.getCourse().getId(), run.getCourse().getCode(), run.getCourse().getName(), run.getPrimaryCoach().getId(), run.getPrimaryCoach().getFullName(), run.getTuitionVnd(), run.getCapacity(), run.getRegistrationOpensAt(), run.getRegistrationClosesAt(), RegistrationStatus.at(now, run.getRegistrationOpensAt(), run.getRegistrationClosesAt()), run.getPlannedStartAt(), run.getPlannedEndAt(), run.getTotalUnits(), run.getStatus());
	}
}
