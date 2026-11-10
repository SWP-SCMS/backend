package com.scms.backend.scheduling.coachleave;

import java.time.Instant;

record CoachLeaveCreateRequest(Instant startTime, Instant endTime, String reason) {
}
