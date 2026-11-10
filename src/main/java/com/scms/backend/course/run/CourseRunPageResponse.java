package com.scms.backend.course.run;

import com.scms.backend.course.CourseRun;
import java.util.List;

public record CourseRunPageResponse(List<CourseRunResponse> content, int page, int size, long totalElements, int totalPages) {

	public static CourseRunPageResponse from(org.springframework.data.domain.Page<CourseRun> page) {
		return new CourseRunPageResponse(page.getContent().stream().map(CourseRunResponse::from).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
