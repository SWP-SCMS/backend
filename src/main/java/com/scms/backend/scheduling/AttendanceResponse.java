package com.scms.backend.scheduling;
import java.time.Instant; import java.util.UUID;
public record AttendanceResponse(UUID id,UUID bookingId,UUID memberId,String memberCode,String fullName,AttendanceStatus status,Instant recordedAt,UUID centerVisitId){static AttendanceResponse from(Attendance a,BookingRepository.AttendanceMemberView m){return new AttendanceResponse(a.getId(),a.getBookingId(),m.getMemberId(),m.getMemberCode(),m.getFullName(),a.getStatus(),a.getRecordedAt(),a.getCenterVisitId());}}
