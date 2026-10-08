package com.scms.backend.manager;

import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/manager/staff-accounts")
public class StaffAccountController {

	private final StaffAccountService service;
	private final StaffAccountCreateService createService;
	private final StaffStatusService statusService;
	private final ResetPasswordService resetPasswordService;

	StaffAccountController(StaffAccountService service, StaffAccountCreateService createService,
			StaffStatusService statusService, ResetPasswordService resetPasswordService) {
		this.service = service;
		this.createService = createService;
		this.statusService = statusService;
		this.resetPasswordService = resetPasswordService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<StaffAccountResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody StaffAccountCreateRequest request) {
		return ResponseEntity.status(201)
			.body(createService.create(UUID.fromString(jwt.getSubject()), request));
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

	@GetMapping("/{accountId}")
	ResponseEntity<StaffAccountResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId) {
		return ResponseEntity.ok(service.get(UUID.fromString(jwt.getSubject()), accountId));
	}

	@PatchMapping("/{accountId}")
	ResponseEntity<StaffAccountResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId,
			@RequestBody StaffAccountPatchRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), accountId, request));
	}

	@PatchMapping("/{accountId}/status")
	ResponseEntity<StaffAccountResponse> changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId,
			@RequestBody StaffStatusChangeRequest request) {
		return ResponseEntity.ok(statusService.deactivate(UUID.fromString(jwt.getSubject()), accountId, request));
	}

	@PostMapping("/{accountId}/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	ResponseEntity<Void> resetPassword(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId) {
		resetPasswordService.managerReset(UUID.fromString(jwt.getSubject()), accountId);
		return ResponseEntity.noContent().build();
	}
}
