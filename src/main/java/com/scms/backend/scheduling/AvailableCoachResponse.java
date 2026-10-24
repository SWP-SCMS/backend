package com.scms.backend.scheduling;

import java.util.UUID;

public record AvailableCoachResponse(UUID coachAccountId, String fullName, String phone) {

	static AvailableCoachResponse from(ClassSessionRepository.AvailableCoachView coach) {
		return new AvailableCoachResponse(coach.getCoachAccountId(), coach.getFullName(), coach.getPhone());
	}
}
