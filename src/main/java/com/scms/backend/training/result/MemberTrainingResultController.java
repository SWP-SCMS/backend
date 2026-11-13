package com.scms.backend.training.result;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US47 - Member - View Training Result.
 */
@RestController
@RequestMapping("/members/me/training-results")
public class MemberTrainingResultController {

	private final MemberTrainingResultService service;

	public MemberTrainingResultController(MemberTrainingResultService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<MemberTrainingResultPageResponse> viewOwnHistory(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) ResultType type, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		validatePagination(page, size);
		var memberId = UUID.fromString(jwt.getSubject());
		var pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(type == null ? service.viewOwnHistory(memberId, pageable) : service.viewOwnHistory(memberId, type, pageable));
	}

	private void validatePagination(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be between 1 and 100");
		}
	}
}
