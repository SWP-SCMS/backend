package com.scms.backend.membership;

import java.util.UUID;
import com.scms.backend.membership.CourseReconciliationModels.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course-payments")
class CourseReconciliationController {
 private final CourseReconciliationService service;
 CourseReconciliationController(CourseReconciliationService service) { this.service = service; }
 @GetMapping("/reconciliation-queue")
 @Operation(description="Active Manager/Reception only. Course payments awaiting reconciliation; expired reservations are never revived.")
 Page queue(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue="ALL") ReconciliationQueueStatus status,
   @RequestParam(required=false) String query, @RequestParam(defaultValue="0") int page,
   @RequestParam(defaultValue="20") int size) {
  return service.queue(UUID.fromString(jwt.getSubject()), status, query, page, size);
 }
 @GetMapping("/{paymentId}")
 @Operation(description="Active Manager/Reception only. Persisted destination and current reconciliation eligibility.")
 Detail detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
  return service.detail(UUID.fromString(jwt.getSubject()), paymentId);
 }
 @GetMapping("/{paymentId}/result")
 @Operation(description="Active Manager/Reception or owning Member. Does not expose destination or reconciliation evidence.")
 Result result(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
  return service.result(UUID.fromString(jwt.getSubject()), paymentId);
 }
 @PostMapping("/{paymentId}/reconcile")
 @Operation(description="Active Manager/Reception. PAID requires a live hold and exact amount/reference; FAILED requires an expired pending payment. Same actor/payment/key/payload replays the saved result; changed payload returns 409.")
 Result reconcile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
   @RequestHeader("Idempotency-Key") String key, @RequestBody PaymentActionRequest request) {
  return service.reconcile(UUID.fromString(jwt.getSubject()), paymentId, key, request);
 }
}
