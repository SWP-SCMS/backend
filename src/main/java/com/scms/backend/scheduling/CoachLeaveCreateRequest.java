package com.scms.backend.scheduling;

import java.time.Instant;

record CoachLeaveCreateRequest(Instant startTime, Instant endTime, String reason) { }

