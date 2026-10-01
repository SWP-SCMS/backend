package com.scms.backend.manager;

import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/staff-accounts")
public class StaffAccountController {

	private final StaffAccountService service;

	StaffAccountController(StaffAccountService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<StaffAccountPageResponse> search(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String query, @RequestParam(required = false) AccountRole role,
			@RequestParam(required = false) AccountStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new StaffAccountValidationException("page", "page must be >= 0 and size must be between 1 and 100");
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending().and(Sort.by("id")));
		return ResponseEntity.ok(service.search(UUID.fromString(jwt.getSubject()), query, role, status, pageable));
	}
}
