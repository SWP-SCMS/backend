package com.scms.backend.course.management;

import com.scms.backend.course.Course;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseSession;
import java.util.LinkedHashMap;
import java.util.Map;

final class CourseManagementSnapshots {

	static Map<String, Object> snapshot(Course course) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("code", course.getCode());
		data.put("name", course.getName());
		data.put("description", course.getDescription());
		data.put("disciplineId", course.getDiscipline().getId());
		data.put("imageUrl", course.getImageUrl());
		data.put("defaultMinimumEnrollment", course.getDefaultMinimumEnrollment());
		data.put("status", course.getStatus().name());
		data.put("submittedByAccountId", course.getSubmittedByAccountId());
		data.put("submittedAt", course.getSubmittedAt() == null ? null : course.getSubmittedAt().toString());
		data.put("reviewedByAccountId", course.getReviewedByAccountId());
		data.put("reviewedAt", course.getReviewedAt() == null ? null : course.getReviewedAt().toString());
		data.put("reviewReason", course.getReviewReason());
		return data;
	}

	static Map<String, Object> snapshot(CourseRun run) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("runCode", run.getRunCode());
		data.put("courseId", run.getCourse().getId());
		data.put("primaryCoachId", run.getPrimaryCoach().getId());
		data.put("tuitionVnd", run.getTuitionVnd());
		data.put("capacity", run.getCapacity());
		data.put("minimumEnrollment", run.getMinimumEnrollment());
		data.put("registrationOpensAt", run.getRegistrationOpensAt().toString());
		data.put("registrationClosesAt", run.getRegistrationClosesAt().toString());
		data.put("plannedStartAt", run.getPlannedStartAt().toString());
		data.put("plannedEndAt", run.getPlannedEndAt().toString());
		data.put("totalUnits", run.getTotalUnits());
		data.put("namedCoachCommitment", run.isNamedCoachCommitment());
		data.put("policyVersionSnapshot", run.getPolicyVersionSnapshot());
		data.put("status", run.getStatus().name());
		return data;
	}

	static Map<String, Object> snapshot(CourseSession session) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("runId", session.getCourseRun().getId());
		data.put("unitNumber", session.getUnitNumber());
		data.put("roomId", session.getRoom().getId());
		data.put("coachId", session.getCoach().getId());
		data.put("startAt", session.getStartAt().toString());
		data.put("endAt", session.getEndAt().toString());
		data.put("allocationVnd", session.getAllocationVnd());
		data.put("status", session.getStatus().name());
		return data;
	}
}
