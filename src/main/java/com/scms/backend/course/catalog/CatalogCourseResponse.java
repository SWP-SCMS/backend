package com.scms.backend.course.catalog;

import com.scms.backend.course.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CatalogCourseResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String description, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID disciplineId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String disciplineName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String imageUrl) {

	static CatalogCourseResponse from(Course course) {
		return new CatalogCourseResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(), course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl());
	}
}
