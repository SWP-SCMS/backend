package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/payments/{paymentId}/evidence-images")
class PaymentEvidenceController {
    private final PaymentEvidenceService service;
    PaymentEvidenceController(PaymentEvidenceService service) { this.service = service; }

    @GetMapping
    List<PaymentEvidenceService.Evidence> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return service.list(UUID.fromString(jwt.getSubject()), paymentId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    PaymentEvidenceService.Evidence upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
            @RequestPart("file") MultipartFile file) {
        return service.upload(UUID.fromString(jwt.getSubject()), paymentId, file);
    }

    @GetMapping("/{evidenceId}")
    ResponseEntity<byte[]> read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId, @PathVariable UUID evidenceId) {
        var image = service.read(UUID.fromString(jwt.getSubject()), paymentId, evidenceId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=chung-tu." + (image.contentType().equals("image/png") ? "png" : "jpg"))
                .body(image.bytes());
    }
}
