package com.scms.backend.course;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class CourseController {

	private final CourseManagementService service;
	private final CourseSessionDisruptionService disruptions;

	CourseController(CourseManagementService service, CourseSessionDisruptionService disruptions) {
		this.service = service;
		this.disruptions = disruptions;
	}

	@GetMapping("/courses")
	ResponseEntity<CoursePageResponse> listCourses(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listCourses(accountId(jwt), page(page, size, "name")));
	}

	@PostMapping("/courses")
	ResponseEntity<CourseResponse> createCourse(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CourseCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createCourse(accountId(jwt), request));
	}

	@GetMapping("/courses/{courseId}")
	ResponseEntity<CourseResponse> getCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId) {
		return ResponseEntity.ok(service.getCourse(accountId(jwt), courseId));
	}

	@PatchMapping("/courses/{courseId}")
	ResponseEntity<CourseResponse> patchCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
			@Valid @RequestBody CoursePatchRequest request) {
		return ResponseEntity.ok(service.patchCourse(accountId(jwt), courseId, request));
	}

	@PostMapping("/courses/{courseId}/publish")
	ResponseEntity<CourseResponse> publishCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
			@Valid @RequestBody VersionRequest request) {
		return ResponseEntity.ok(service.publishCourse(accountId(jwt), courseId, request));
	}

	@PostMapping("/courses/{courseId}/deactivate")
	ResponseEntity<CourseResponse> deactivateCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
			@Valid @RequestBody VersionRequest request) {
		return ResponseEntity.ok(service.deactivateCourse(accountId(jwt), courseId, request));
	}

	@PostMapping("/courses/{courseId}/archive")
	ResponseEntity<CourseResponse> archiveCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
			@Valid @RequestBody VersionRequest request) {
		return ResponseEntity.ok(service.archiveCourse(accountId(jwt), courseId, request));
	}

	@GetMapping("/course-runs")
	ResponseEntity<CourseRunPageResponse> listRuns(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listRuns(accountId(jwt), page(page, size, "plannedStartAt")));
	}

	@PostMapping("/course-runs")
	ResponseEntity<CourseRunResponse> createRun(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CourseRunCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createRun(accountId(jwt), request));
	}

	@GetMapping("/course-runs/{runId}")
	ResponseEntity<CourseRunResponse> getRun(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId) {
		return ResponseEntity.ok(service.getRun(accountId(jwt), runId));
	}

	@PatchMapping("/course-runs/{runId}")
	ResponseEntity<CourseRunResponse> patchRun(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@Valid @RequestBody CourseRunPatchRequest request) {
		return ResponseEntity.ok(service.patchRun(accountId(jwt), runId, request));
	}

	@PostMapping("/course-runs/{runId}/open-registration")
	ResponseEntity<CourseRunResponse> openRun(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@Valid @RequestBody CourseRunOpenRequest request) {
		return ResponseEntity.ok(service.openRun(accountId(jwt), runId, request));
	}

	@PostMapping("/course-runs/{runId}/cancel")
	ResponseEntity<CourseRunResponse> cancelRun(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@Valid @RequestBody VersionRequest request) {
		return ResponseEntity.ok(service.cancelRun(accountId(jwt), runId, request));
	}

	@GetMapping("/course-runs/{runId}/sessions")
	ResponseEntity<List<CourseSessionResponse>> listSessions(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID runId) {
		return ResponseEntity.ok(service.listSessions(accountId(jwt), runId));
	}

	@PostMapping("/course-runs/{runId}/sessions")
	ResponseEntity<CourseSessionResponse> createSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@Valid @RequestBody CourseSessionCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createSession(accountId(jwt), runId, request));
	}

	@GetMapping("/course-runs/{runId}/sessions/{sessionId}")
	ResponseEntity<CourseSessionResponse> getSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@PathVariable UUID sessionId) {
		return ResponseEntity.ok(service.getSession(accountId(jwt), runId, sessionId));
	}

	@PatchMapping("/course-runs/{runId}/sessions/{sessionId}")
	ResponseEntity<CourseSessionResponse> patchSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@PathVariable UUID sessionId, @Valid @RequestBody CourseSessionPatchRequest request) {
		return ResponseEntity.ok(service.patchSession(accountId(jwt), runId, sessionId, request));
	}

	@PostMapping("/course-runs/{runId}/sessions/{sessionId}/cancel")
	ResponseEntity<CourseSessionResponse> cancelSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId,
			@PathVariable UUID sessionId, @Valid @RequestBody CourseSessionDisruptionRequest request) {
		return ResponseEntity.ok(disruptions.cancel(accountId(jwt), runId, sessionId,
			request == null ? null : request.expectedVersion()));
	}

	@PostMapping("/course-runs/{runId}/sessions/{sessionId}/makeup")
	ResponseEntity<CourseSessionDisruptionService.CourseMakeupResponse> scheduleMakeup(
			@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId, @PathVariable UUID sessionId,
			@Valid @RequestBody CourseMakeupRequest request) {
		return ResponseEntity.ok(disruptions.scheduleMakeup(accountId(jwt), runId, sessionId, request));
	}

	private PageRequest page(int page, int size, String property) {
		if (page < 0) throw CourseException.validation("page", "must be greater than or equal to zero");
		if (size < 1 || size > 100) throw CourseException.validation("size", "must be between 1 and 100");
		return PageRequest.of(page, size, Sort.by(property).ascending().and(Sort.by("id")));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
