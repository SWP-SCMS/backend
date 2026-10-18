package com.scms.backend.training;

import java.util.List;
import java.util.UUID;

import com.scms.backend.manager.StaffAccountValidationException;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class AssignmentDiscoveryController {

	private final AssignmentDiscoveryService service;

	public AssignmentDiscoveryController(AssignmentDiscoveryService service) {
		this.service = service;
	}

	@GetMapping(value = "/coach-assignments/candidates", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AssignmentCandidatePageResponse> candidates(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String query,
			@Parameter(schema = @Schema(type = "string", allowableValues = {
				"UNASSIGNED", "ASSIGNED", "ALL" }, defaultValue = "ALL"))
			@RequestParam(required = false) String assignmentStatus,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		validatePagination(page, size);
		return ResponseEntity.ok(service.candidates(accountId(jwt), query, parseAssignmentStatus(assignmentStatus),
			PageRequest.of(page, size)));
	}

	@GetMapping(value = "/coaches/assignment-workloads", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<CoachAssignmentWorkloadResponse>> workloads(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.workloads(accountId(jwt)));
	}

	private void validatePagination(int page, int size) {
		if (page < 0) {
			throw new StaffAccountValidationException("page", "must be greater than or equal to 0");
		}
		if (size < 1 || size > 100) {
			throw new StaffAccountValidationException("size", "must be between 1 and 100");
		}
	}

	private AssignmentCandidateStatus parseAssignmentStatus(String value) {
		if (value == null) return AssignmentCandidateStatus.ALL;
		try {
			return AssignmentCandidateStatus.valueOf(value);
		}
		catch (IllegalArgumentException exception) {
			throw new TypeMismatchException(value, AssignmentCandidateStatus.class, exception);
		}
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
