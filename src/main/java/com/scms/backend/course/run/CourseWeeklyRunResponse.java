package com.scms.backend.course.run;

import com.scms.backend.course.session.CourseSessionResponse;
import java.util.List;

public record CourseWeeklyRunResponse(CourseRunResponse run, List<CourseSessionResponse> sessions) {
}
