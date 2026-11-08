package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/coach-leave-requests/{requestId}")
class CoachLeaveImpactController {
    private final CoachLeaveImpactService service;
    CoachLeaveImpactController(CoachLeaveImpactService service) { this.service = service; }

    @GetMapping("/impact")
    CoachLeaveImpactService.Impact detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return service.detail(UUID.fromString(jwt.getSubject()), requestId);
    }

    @GetMapping("/affected-sessions/{type}/{sessionId}/available-coaches")
    List<AvailableCoachResponse> available(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
            @PathVariable String type, @PathVariable UUID sessionId) {
        return service.available(UUID.fromString(jwt.getSubject()), requestId, type, sessionId);
    }
}
