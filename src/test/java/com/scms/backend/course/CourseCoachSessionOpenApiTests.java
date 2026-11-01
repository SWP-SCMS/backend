package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseCoachSessionOpenApiTests {
	@Test
	void staticContractDocumentsCoachCourseSessionDiscovery() throws Exception {
		var source = getClass().getResourceAsStream("/openapi/course-coach-session.yaml");
		assertThat(source).isNotNull();
		JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(source);
		assertThat(document.path("paths").properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("/coach/course-sessions", "/coach/course-sessions/{sessionId}");
		assertThat(document.at("/paths/~1coach~1course-sessions/get/parameters").toString())
			.contains("from", "to", "status", "page", "size", "sort");
		assertThat(document.at("/components/schemas/CourseCoachSession/properties").properties()
			.stream().map(java.util.Map.Entry::getKey).toList())
			.contains("courseSessionId", "courseId", "courseRunId", "startsAt", "endsAt", "roomId",
				"sessionStatus", "rosterCount", "attendanceWritable", "attendanceWriteReason");
		assertThat(document.at("/components/schemas/AttendanceWriteReason/enum").toString())
			.contains("WRITABLE", "TOO_EARLY", "WINDOW_CLOSED", "SESSION_CANCELLED",
				"NOT_ASSIGNED_COACH", "COURSE_RUN_NOT_ACTIVE");
	}
}
