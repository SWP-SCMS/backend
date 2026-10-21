package com.scms.backend.emailverification;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/manager/email-verification/manual-activation-requests")
public class ManagerEmailVerificationController {
	private final ManualActivationService service;
	ManagerEmailVerificationController(ManualActivationService service){this.service=service;}
	@GetMapping ResponseEntity<ManualActivationPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@Parameter(schema=@Schema(defaultValue="PENDING")) @RequestParam(defaultValue="PENDING") ManualActivationStatus status,
			@Parameter(schema=@Schema(minimum="0",defaultValue="0")) @RequestParam(defaultValue="0") int page,
			@Parameter(schema=@Schema(minimum="1",maximum="100",defaultValue="20")) @RequestParam(defaultValue="20") int size){return ResponseEntity.ok(service.list(id(jwt),com.scms.backend.account.AccountRole.MANAGER,status,page,size));}
	@GetMapping("/{requestId}") ResponseEntity<ManualActivationResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId){return ResponseEntity.ok(service.getForActor(id(jwt),requestId,com.scms.backend.account.AccountRole.MANAGER));}
	@PostMapping("/{requestId}/approve") ResponseEntity<ManualActivationResponse> approve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId,@Valid @RequestBody ManualActivationDecisionRequest request){return ResponseEntity.ok(service.approve(id(jwt),requestId,request));}
	@PostMapping("/{requestId}/reject") ResponseEntity<ManualActivationResponse> reject(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId,@Valid @RequestBody ManualActivationDecisionRequest request){return ResponseEntity.ok(service.reject(id(jwt),requestId,request));}
	private UUID id(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
