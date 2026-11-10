package com.scms.backend.course.run;

import com.scms.backend.course.session.CourseSessionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record CourseRunRescheduleResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CourseRunResponse run, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CourseSessionResponse> sessions, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID changeEventId) {
}
