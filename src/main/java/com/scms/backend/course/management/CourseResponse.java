package com.scms.backend.course.management;

import com.scms.backend.course.Course;
import com.scms.backend.course.CourseStatus;
import java.time.Instant;
import java.util.UUID;

public record CourseResponse(UUID id, String code, String name, String description, UUID disciplineId, String disciplineName, String imageUrl, Integer defaultMinimumEnrollment, CourseStatus status, Instant createdAt, Instant updatedAt, Long version) {

	static CourseResponse from(Course course) {
		return new CourseResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(), course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl(), course.getDefaultMinimumEnrollment(), course.getStatus(), course.getCreatedAt(), course.getUpdatedAt(), course.getVersion());
	}
}
