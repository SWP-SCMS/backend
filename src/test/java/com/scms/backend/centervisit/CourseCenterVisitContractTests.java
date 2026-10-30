package com.scms.backend.centervisit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class CourseCenterVisitContractTests {

	@Test
	void migrationEnforcesExactlyOneVisitSourceAndCourseLinkage() throws Exception {
		String sql = Files.readString(Path.of("src/main/resources/db/migration/V56__course_center_visits.sql"));

		assertThat(sql).contains("alter column membership_id drop not null",
			"fk_center_visits_course_enrollment_session", "chk_center_visits_source",
			"uq_course_enrollments_visit_owner", "fk_center_visits_course_enrollment_owner",
			"foreign key (course_enrollment_id, member_account_id)",
			"visit_source = 'MEMBERSHIP'", "visit_source = 'COURSE'");
	}

	@Test
	void courseVisitEligibilityUsesHalfOpenWindowLocksEligibilityAndAllowsCompletedSession() throws Exception {
		String sql = query("findEligibleCourseEnrollmentId");

		assertThat(sql).contains(":at >= s.start_at - interval '30 minutes'",
			":at < s.end_at + interval '30 minutes'", "'COMPLETED'", "for update of e, s");
	}

	@Test
	void membershipLifecycleAndGymAttendanceIgnoreCourseVisits() throws Exception {
		assertThat(query("checkoutOpenForMembershipTermination")).contains("visit_source = 'MEMBERSHIP'");
		assertThat(query("checkoutOpenForMembershipFreeze")).contains("visit_source = 'MEMBERSHIP'");
		assertThat(query("findOverlappingVisitId")).contains("visit_source = 'MEMBERSHIP'");
	}

	private String query(String name) {
		for (Method method : CenterVisitRepository.class.getDeclaredMethods()) {
			if (method.getName().equals(name)) return method.getAnnotation(Query.class).value();
		}
		throw new AssertionError("Missing repository method " + name);
	}
}
