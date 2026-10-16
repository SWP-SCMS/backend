package com.scms.backend.training;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US47 - Member views their own Personal and Session Training Result history. */
@Service
public class MemberTrainingResultService {

	private final TrainingResultRepository results;

	public MemberTrainingResultService(TrainingResultRepository results) {
		this.results = results;
	}

	@Transactional(readOnly = true)
	public List<MemberTrainingResultResponse> viewOwnHistory(UUID memberId) {
		return results.findHistoryByMemberAccountId(memberId).stream()
			.map(MemberTrainingResultResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public MemberTrainingResultPageResponse viewOwnHistory(UUID memberId, Pageable pageable) {
		var page = results.findHistoryByMemberAccountId(memberId, pageable);
		return new MemberTrainingResultPageResponse(page.getContent().stream()
				.map(MemberTrainingResultResponse::from).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}
}
