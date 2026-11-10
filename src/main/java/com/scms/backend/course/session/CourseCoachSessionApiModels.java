package com.scms.backend.course.session;

import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CourseCoachSessionApiModels {

	private CourseCoachSessionApiModels() {
	}

	public enum AttendanceWriteReason {

		WRITABLE,
		TOO_EARLY,
		WINDOW_CLOSED,
		SESSION_CANCELLED,
		NOT_ASSIGNED_COACH,
		COURSE_RUN_NOT_ACTIVE
	}

	public record CourseCoachSessionResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseSessionId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int unitNumber, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endsAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseRunId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String runCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CourseRunStatus courseRunStatus, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID coachId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String coachName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID roomId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String roomCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String roomName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CourseSessionStatus sessionStatus, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long rosterCount, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Assigned Coach may record attendance until end plus 30 minutes, including automatically completed sessions and Runs.") boolean attendanceWritable, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AttendanceWriteReason attendanceWriteReason) {
	}

	public record CourseCoachSessionPage(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CourseCoachSessionResponse> content, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {
	}
}
