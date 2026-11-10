package com.scms.backend.course.run;

import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record CourseRunResponse(UUID id, String runCode, UUID courseId, String courseCode, String courseName, UUID primaryCoachId, String primaryCoachName, BigInteger tuitionVnd, int capacity, int minimumEnrollment, Instant registrationOpensAt, Instant registrationClosesAt, Instant plannedStartAt, Instant plannedEndAt, int totalUnits, boolean namedCoachCommitment, String policyVersionSnapshot, CourseRunStatus status, Instant createdAt, Instant updatedAt, Long version) {

	public static CourseRunResponse from(CourseRun run) {
		return new CourseRunResponse(run.getId(), run.getRunCode(), run.getCourse().getId(), run.getCourse().getCode(), run.getCourse().getName(), run.getPrimaryCoach().getId(), run.getPrimaryCoach().getFullName(), run.getTuitionVnd(), run.getCapacity(), run.getMinimumEnrollment(), run.getRegistrationOpensAt(), run.getRegistrationClosesAt(), run.getPlannedStartAt(), run.getPlannedEndAt(), run.getTotalUnits(), run.isNamedCoachCommitment(), run.getPolicyVersionSnapshot(), run.getStatus(), run.getCreatedAt(), run.getUpdatedAt(), run.getVersion());
	}
}
