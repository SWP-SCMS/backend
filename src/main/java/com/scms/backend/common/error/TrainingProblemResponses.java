package com.scms.backend.common.error;

import com.scms.backend.training.CoachAssignmentException;
import com.scms.backend.training.TrainingFeedbackException;
import com.scms.backend.training.TrainingPlanException;
import com.scms.backend.training.TrainingResultException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

final class TrainingProblemResponses {

	static ProblemDetail handleCoachAssignment(CoachAssignmentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Coach assignment request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (exception.getImpact() != null) {
			problem.setProperty("impact", exception.getImpact());
		}
		return problem;
	}

	static ProblemDetail handleTrainingPlan(TrainingPlanException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Training Plan request failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (exception.getCurrentVersionNumber() != null) {
			problem.setProperty("currentVersionNumber", exception.getCurrentVersionNumber());
		}
		if (exception.getActivePlanId() != null) {
			problem.setProperty("activePlanId", exception.getActivePlanId());
			problem.setProperty("activePlanTitle", exception.getActivePlanTitle());
		}
		return problem;
	}

	static ProblemDetail handleTrainingResult(TrainingResultException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Training Result request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handleTrainingFeedback(TrainingFeedbackException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Training Feedback request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}
}
