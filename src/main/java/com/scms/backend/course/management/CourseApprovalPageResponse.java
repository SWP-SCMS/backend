package com.scms.backend.course.management;

import com.scms.backend.course.Course;
import java.util.List;

record CourseApprovalPageResponse(List<CourseApprovalResponse> content, int page, int size, long totalElements, int totalPages) {

	static CourseApprovalPageResponse from(org.springframework.data.domain.Page<Course> page) {
		return new CourseApprovalPageResponse(page.getContent().stream().map(CourseApprovalResponse::from).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
