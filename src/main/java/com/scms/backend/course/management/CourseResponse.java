package com.scms.backend.course.management;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.scms.backend.course.Course;
import com.scms.backend.course.CourseStatus;
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

public record CourseResponse(UUID id, String code, String name, String description, UUID disciplineId, String disciplineName, String imageUrl, Integer defaultMinimumEnrollment, CourseStatus status, Instant createdAt, Instant updatedAt, Long version) {

	static CourseResponse from(Course course) {
		return new CourseResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(), course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl(), course.getDefaultMinimumEnrollment(), course.getStatus(), course.getCreatedAt(), course.getUpdatedAt(), course.getVersion());
	}
}
