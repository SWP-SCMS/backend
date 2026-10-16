package com.scms.backend.training;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US50 - Member views their own Personal and Session Training Feedback history. */
@Service
public class MemberTrainingFeedbackService {

	private final TrainingFeedbackRepository feedback;

	public MemberTrainingFeedbackService(TrainingFeedbackRepository feedback) {
		this.feedback = feedback;
	}

	@Transactional(readOnly = true)
	public List<MemberTrainingFeedbackResponse> viewOwnHistory(UUID memberId) {
		return feedback.findHistoryByMemberAccountId(memberId).stream()
			.map(view -> MemberTrainingFeedbackResponse.from(view.getFeedback(), view.getClassSessionId()))
			.toList();
	}

	@Transactional(readOnly = true)
	public MemberTrainingFeedbackPageResponse viewOwnHistory(UUID memberId, Pageable pageable) {
		var page = feedback.findHistoryByMemberAccountId(memberId, pageable);
		return new MemberTrainingFeedbackPageResponse(page.getContent().stream()
				.map(view -> MemberTrainingFeedbackResponse.from(view.getFeedback(), view.getClassSessionId()))
				.toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
