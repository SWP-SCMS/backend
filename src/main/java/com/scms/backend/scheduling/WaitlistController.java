package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class WaitlistController {
	private final WaitlistService service;
	WaitlistController(WaitlistService service){this.service=service;}
	@PostMapping("/members/class-sessions/{sessionId}/waitlist") ResponseEntity<WaitlistResponse> join(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID sessionId){return ResponseEntity.status(201).body(service.join(subject(jwt),sessionId));}
	@DeleteMapping("/members/class-sessions/{sessionId}/waitlist") ResponseEntity<WaitlistResponse> leave(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID sessionId){return ResponseEntity.ok(service.leave(subject(jwt),sessionId));}
	@GetMapping("/members/me/waitlists") ResponseEntity<List<WaitlistResponse>> mine(@AuthenticationPrincipal Jwt jwt){return ResponseEntity.ok(service.mine(subject(jwt)));}
	@PostMapping("/reception/members/{memberId}/class-sessions/{sessionId}/waitlist") ResponseEntity<WaitlistResponse> joinFor(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID memberId,@PathVariable UUID sessionId){return ResponseEntity.status(201).body(service.joinForMember(subject(jwt),memberId,sessionId));}
	@DeleteMapping("/reception/members/{memberId}/class-sessions/{sessionId}/waitlist") ResponseEntity<WaitlistResponse> leaveFor(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID memberId,@PathVariable UUID sessionId){return ResponseEntity.ok(service.leaveForMember(subject(jwt),memberId,sessionId));}
	@GetMapping("/reception/class-sessions/{sessionId}/waitlist") ResponseEntity<List<WaitlistResponse>> reception(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID sessionId){return ResponseEntity.ok(service.sessionList(subject(jwt),AccountRole.RECEPTIONIST,sessionId));}
	@GetMapping("/manager/class-sessions/{sessionId}/waitlist") ResponseEntity<List<WaitlistResponse>> manager(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID sessionId){return ResponseEntity.ok(service.sessionList(subject(jwt),AccountRole.MANAGER,sessionId));}
	private UUID subject(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
