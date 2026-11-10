package com.scms.backend.course.catalog;

import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CatalogCourseSessionResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int unitNumber, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID roomId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String roomName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID coachId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String coachName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant originalStartAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant originalEndAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9999999999999999999") BigInteger allocationVnd, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "SCHEDULED", "CANCELLED", "CANCELLED_PENDING_MAKEUP", "MAKEUP_SCHEDULED", "COMPLETED", "UNRESOLVED", "RESOLVED_BY_REFUND" }) CourseSessionStatus status) {

	static CatalogCourseSessionResponse from(CourseSession session) {
		return new CatalogCourseSessionResponse(session.getId(), session.getUnitNumber(), session.getRoom().getId(), session.getRoom().getName(), session.getCoach().getId(), session.getCoach().getFullName(), session.getStartAt(), session.getEndAt(), session.getOriginalStartAt(), session.getOriginalEndAt(), session.getAllocationVnd(), session.getStatus());
	}
}
