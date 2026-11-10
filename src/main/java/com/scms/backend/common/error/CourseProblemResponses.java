package com.scms.backend.common.error;

import com.scms.backend.course.CourseAttendanceException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseOrderException;
import com.scms.backend.course.CourseRefundException;
import com.scms.backend.course.CourseTransferException;
import com.scms.backend.course.CourseWaitlistException;
import com.scms.backend.course.CourseWithdrawalException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

final class CourseProblemResponses {

	static ProblemDetail handleCourse(CourseException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Course request failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		return problem;
	}

	static ProblemDetail handleCourseOrder(CourseOrderException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Course order request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handleCourseWaitlist(CourseWaitlistException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Course waitlist request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handleCourseTransfer(CourseTransferException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle("Course transfer request failed");
		problem.setProperty("code", exception.code());
		return problem;
	}

	static ProblemDetail handleCourseWithdrawal(CourseWithdrawalException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle("Course withdrawal request failed");
		problem.setProperty("code", exception.code());
		return problem;
	}

	static ProblemDetail handleCourseRefund(CourseRefundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle("Course refund request failed");
		problem.setProperty("code", exception.code());
		return problem;
	}

	static ProblemDetail handleCourseAttendance(CourseAttendanceException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Course attendance request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}
}
