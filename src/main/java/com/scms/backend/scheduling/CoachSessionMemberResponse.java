package com.scms.backend.scheduling;

import java.util.UUID;

public record CoachSessionMemberResponse(UUID memberId, String memberCode, String fullName, String phone,
		String profileImageUrl) {

	static CoachSessionMemberResponse from(BookingRepository.BookedMemberView member) {
		return new CoachSessionMemberResponse(member.getMemberId(), member.getMemberCode(), member.getFullName(),
			member.getPhone(), member.getProfileImageUrl());
	}
}
