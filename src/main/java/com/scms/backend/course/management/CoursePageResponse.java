package com.scms.backend.course.management;

import com.scms.backend.course.Course;
import java.util.List;

record CoursePageResponse(List<CourseResponse> content, int page, int size, long totalElements, int totalPages) {

	static CoursePageResponse from(org.springframework.data.domain.Page<Course> page) {
		return new CoursePageResponse(page.getContent().stream().map(CourseResponse::from).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
