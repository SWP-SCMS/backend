package com.scms.backend.scheduling.booking;

import com.scms.backend.scheduling.Booking;
import com.scms.backend.scheduling.BookingStatus;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID id, UUID sessionId, UUID memberId, UUID membershipId, BookingStatus status, UUID bookedBy, Instant bookedAt, UUID cancelledBy, String cancellationSource, Instant cancelledAt) {

	static BookingResponse from(Booking booking) {
		return new BookingResponse(booking.getId(), booking.getClassSessionId(), booking.getMemberAccountId(), booking.getMembershipId(), booking.getStatus(), booking.getBookedByAccountId(), booking.getCreatedAt(), booking.getCancelledByAccountId(), booking.getCancellationSource(), booking.getCancelledAt());
	}
}
