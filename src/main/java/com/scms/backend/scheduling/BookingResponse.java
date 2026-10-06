package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID id, UUID sessionId, UUID memberId, UUID membershipId, BookingStatus status,
		UUID bookedBy, Instant bookedAt) {

	static BookingResponse from(Booking booking) {
		return new BookingResponse(booking.getId(), booking.getClassSessionId(), booking.getMemberAccountId(),
			booking.getMembershipId(), booking.getStatus(), booking.getBookedByAccountId(), booking.getCreatedAt());
	}
}
