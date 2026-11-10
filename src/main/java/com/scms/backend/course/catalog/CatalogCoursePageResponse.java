package com.scms.backend.course.catalog;

import com.scms.backend.course.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CatalogCoursePageResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CatalogCourseResponse> content, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {

	static CatalogCoursePageResponse from(org.springframework.data.domain.Page<Course> page) {
		return new CatalogCoursePageResponse(page.getContent().stream().map(CatalogCourseResponse::from).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
