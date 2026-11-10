package com.scms.backend.training.result;

import com.scms.backend.scheduling.SportClassType;
import com.scms.backend.training.TrainingResult;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MemberTrainingResultResponse(UUID id, UUID memberAccountId, UUID coachAssignmentId, UUID trainingPlanVersionId, UUID classSessionId, Instant activityAt, Map<String, Object> content, UUID createdByAccountId, String coachName, String sessionName, Instant sessionStartTime, String className, Instant createdAt, Instant updatedAt, UUID courseSessionId) {

	static MemberTrainingResultResponse from(TrainingResultRepository.MemberTrainingResultHistoryView view) {
		TrainingResult result = view.getResult();
		return new MemberTrainingResultResponse(result.getId(), result.getMemberAccountId(), result.getCoachAssignmentId(), result.getTrainingPlanVersionId(), result.getClassSessionId(), result.getActivityAt(), result.getContent(), result.getCreatedByAccountId(), view.getCoachName(), view.getSessionName(), view.getSessionStartTime(), view.getClassType() == SportClassType.PT_1_1 ? null : view.getSessionName(), result.getCreatedAt(), result.getUpdatedAt(), result.getCourseSessionId());
	}
}
