package com.scms.backend.scheduling;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/coach-leave-requests/{requestId}/batch")
class CoachLeaveBatchController {
    private final CoachLeaveBatchService service;
    CoachLeaveBatchController(CoachLeaveBatchService service) { this.service=service; }
    @PostMapping("/available-coaches")
    List<AvailableCoachResponse> available(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
            @RequestBody CoachLeaveBatchService.Command request) {
        return service.available(UUID.fromString(jwt.getSubject()),requestId,request);
    }
    @PostMapping
    CoachLeaveBatchService.Result resolve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
            @RequestBody CoachLeaveBatchService.Command request) {
        return service.resolve(UUID.fromString(jwt.getSubject()),requestId,request);
    }
}
