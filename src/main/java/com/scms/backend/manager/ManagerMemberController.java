package com.scms.backend.manager;

import java.util.UUID;

import com.scms.backend.account.AccountStatus;
import com.scms.backend.member.MemberProfilePatchRequest;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/members")
public class ManagerMemberController {

	private final ManagerMemberService service;

	ManagerMemberController(ManagerMemberService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<MemberAccountPageResponse> search(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String query, @RequestParam(required = false) AccountStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new StaffAccountValidationException("page", "page must be >= 0 and size must be between 1 and 100");
		}
		Pageable pageable = PageRequest.of(page, size,
			Sort.by("account.fullName").ascending().and(Sort.by("accountId")));
		return ResponseEntity.ok(service.search(accountId(jwt), query, status, pageable));
	}

	@GetMapping("/{id}")
	ResponseEntity<MemberAccountResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ResponseEntity.ok(service.get(accountId(jwt), id));
	}

	@PatchMapping("/{id}")
	ResponseEntity<MemberAccountResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody MemberProfilePatchRequest request) {
		return ResponseEntity.ok(service.update(accountId(jwt), id, request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
