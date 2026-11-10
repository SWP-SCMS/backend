package com.scms.backend.training.feedback;

import com.scms.backend.scheduling.SportClassType;
import com.scms.backend.training.TrainingFeedback;
import java.time.Instant;
import java.util.UUID;

public record MemberTrainingFeedbackResponse(UUID id, UUID memberAccountId, UUID coachAssignmentId, UUID trainingPlanVersionId, UUID trainingResultId, UUID classSessionId, String content, UUID createdByAccountId, String coachName, String sessionName, Instant sessionStartTime, String className, Instant createdAt, Instant updatedAt, UUID courseSessionId) {

	static MemberTrainingFeedbackResponse from(TrainingFeedbackRepository.MemberFeedbackHistoryView view) {
		TrainingFeedback feedback = view.getFeedback();
		return new MemberTrainingFeedbackResponse(feedback.getId(), feedback.getMemberAccountId(), feedback.getCoachAssignmentId(), feedback.getTrainingPlanVersionId(), feedback.getTrainingResultId(), view.getClassSessionId(), feedback.getContent(), feedback.getCreatedByAccountId(), view.getCoachName(), view.getSessionName(), view.getSessionStartTime(), view.getClassType() == SportClassType.PT_1_1 ? null : view.getSessionName(), feedback.getCreatedAt(), feedback.getUpdatedAt(), view.getCourseSessionId());
	}
}
