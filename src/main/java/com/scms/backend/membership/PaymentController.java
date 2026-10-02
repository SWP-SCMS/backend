package com.scms.backend.membership;
import java.util.UUID; import java.util.Map;
import org.springframework.http.ResponseEntity; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
@RestController class PaymentController { private final PaymentService service; PaymentController(PaymentService service){this.service=service;}
 @PostMapping("/reception/membership-orders/{orderId}/cash") ResponseEntity<Map<String,Object>> cash(@AuthenticationPrincipal Jwt j,@PathVariable UUID orderId){return ResponseEntity.ok(service.cash(id(j),orderId));}
 @PostMapping("/payments/{paymentId}/reconcile") ResponseEntity<Map<String,Object>> reconcile(@AuthenticationPrincipal Jwt j,@PathVariable UUID paymentId,@RequestBody PaymentActionRequest r){return ResponseEntity.ok(service.reconcile(id(j),paymentId,r));}
 private UUID id(Jwt j){return UUID.fromString(j.getSubject());}}
