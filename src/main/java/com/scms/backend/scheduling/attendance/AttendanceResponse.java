package com.scms.backend.scheduling.attendance;

import com.scms.backend.scheduling.Attendance;
import com.scms.backend.scheduling.AttendanceStatus;
import com.scms.backend.scheduling.booking.BookingRepository;
import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(UUID id, UUID bookingId, UUID memberId, String memberCode, String fullName, AttendanceStatus status, Instant recordedAt, UUID centerVisitId, boolean receptionistCheckedIn) {

	static AttendanceResponse from(Attendance a, BookingRepository.AttendanceMemberView m, boolean checkedIn) {
		return new AttendanceResponse(a.getId(), a.getBookingId(), m.getMemberId(), m.getMemberCode(), m.getFullName(), a.getStatus(), a.getRecordedAt(), a.getCenterVisitId(), checkedIn);
	}
}
