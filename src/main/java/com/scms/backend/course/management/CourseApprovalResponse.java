package com.scms.backend.course.management;

import com.scms.backend.course.Course;
import com.scms.backend.course.CourseStatus;
import java.time.Instant;
import java.util.UUID;

record CourseApprovalResponse(UUID id, String code, String name, String description, UUID disciplineId, String disciplineName, String imageUrl, Integer defaultMinimumEnrollment, CourseStatus status, UUID submittedByAccountId, Instant submittedAt, UUID reviewedByAccountId, Instant reviewedAt, String reviewReason, Instant createdAt, Instant updatedAt, Long version) {

	static CourseApprovalResponse from(Course course) {
		return new CourseApprovalResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(), course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl(), course.getDefaultMinimumEnrollment(), course.getStatus(), course.getSubmittedByAccountId(), course.getSubmittedAt(), course.getReviewedByAccountId(), course.getReviewedAt(), course.getReviewReason(), course.getCreatedAt(), course.getUpdatedAt(), course.getVersion());
	}
}
