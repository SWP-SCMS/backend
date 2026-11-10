package com.scms.backend.course.run;

import java.time.Instant;

record CourseWeeklySlot(int unitNumber, Instant startAt, Instant endAt) {
}
