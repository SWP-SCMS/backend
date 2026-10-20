package com.scms.backend.emailverification;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/reception/email-verification")
public class ReceptionEmailVerificationController {
	private final ManualActivationService service;
	ReceptionEmailVerificationController(ManualActivationService service){this.service=service;}
	@GetMapping("/pending-accounts") ResponseEntity<PendingVerificationAccountResponse> search(@AuthenticationPrincipal Jwt jwt,@RequestParam String query){return ResponseEntity.ok(service.search(id(jwt),query));}
	@PostMapping("/manual-activation-requests") @ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<ManualActivationResponse> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ManualActivationCreateRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id(jwt),request));}
	@GetMapping("/manual-activation-requests") ResponseEntity<ManualActivationPageResponse> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="PENDING") ManualActivationStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(service.list(id(jwt),com.scms.backend.account.AccountRole.RECEPTIONIST,status,page,size));}
	@GetMapping("/manual-activation-requests/{requestId}") ResponseEntity<ManualActivationResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId){return ResponseEntity.ok(service.getForActor(id(jwt),requestId,com.scms.backend.account.AccountRole.RECEPTIONIST));}
	private UUID id(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
