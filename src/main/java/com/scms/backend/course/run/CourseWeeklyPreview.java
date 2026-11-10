package com.scms.backend.course.run;

import java.time.Instant;
import java.util.List;

record CourseWeeklyPreview(String timezone, Instant plannedStartAt, Instant plannedEndAt, List<CourseWeeklySlot> sessions) {
}
