package com.scms.backend.course;

import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
class CourseCatalogController {

	private final CourseCatalogService service;

	CourseCatalogController(CourseCatalogService service) {
		this.service = service;
	}

	@GetMapping("/courses")
	@SecurityRequirements
	ResponseEntity<CatalogCoursePageResponse> listCourses(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listCourses(page(page, size, "name")));
	}

	@GetMapping("/courses/{courseId}")
	@SecurityRequirements
	ResponseEntity<CatalogCourseResponse> getCourse(@PathVariable UUID courseId) {
		return ResponseEntity.ok(service.getCourse(courseId));
	}

	@GetMapping("/course-runs")
	@SecurityRequirements
	ResponseEntity<CatalogCourseRunPageResponse> listRuns(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listRuns(page(page, size, "plannedStartAt")));
	}

	@GetMapping("/course-runs/{runId}")
	@SecurityRequirements
	ResponseEntity<CatalogCourseRunDetailResponse> getRun(@PathVariable UUID runId) {
		return ResponseEntity.ok(service.getRun(runId));
	}

	private PageRequest page(int page, int size, String property) {
		if (page < 0) throw CourseException.validation("page", "must be greater than or equal to zero");
		if (size < 1 || size > 100) throw CourseException.validation("size", "must be between 1 and 100");
		return PageRequest.of(page, size, Sort.by(property).ascending().and(Sort.by("id")));
	}
}
