package com.scms.backend.course.session;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseSessionResponse(UUID id, UUID runId, String runCode, int unitNumber, UUID roomId, String roomName, UUID coachId, String coachName, Instant startAt, Instant endAt, Instant originalStartAt, Instant originalEndAt, BigInteger allocationVnd, CourseSessionStatus status, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isMakeup, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) UUID originalSessionId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) UUID replacementSessionId, Instant createdAt, Instant updatedAt, Long version) {

	public static CourseSessionResponse from(CourseSession session) {
		return from(session, null);
	}

	public static CourseSessionResponse from(CourseSession session, UUID originalSessionId) {
		return new CourseSessionResponse(session.getId(), session.getCourseRun().getId(), session.getCourseRun().getRunCode(), session.getUnitNumber(), session.getRoom().getId(), session.getRoom().getName(), session.getCoach().getId(), session.getCoach().getFullName(), session.getStartAt(), session.getEndAt(), session.getOriginalStartAt(), session.getOriginalEndAt(), session.getAllocationVnd(), session.getStatus(), session.isMakeup(), originalSessionId, session.getReplacementSession() == null ? null : session.getReplacementSession().getId(), session.getCreatedAt(), session.getUpdatedAt(), session.getVersion());
	}
}
