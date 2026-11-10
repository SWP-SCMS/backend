package com.scms.backend.course.catalog;

import com.scms.backend.course.CourseRun;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

record CatalogCourseRunPageResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CatalogCourseRunResponse> content, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {

	static CatalogCourseRunPageResponse from(org.springframework.data.domain.Page<CourseRun> page, Instant now) {
		return new CatalogCourseRunPageResponse(page.getContent().stream().map(run -> CatalogCourseRunResponse.from(run, now)).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
