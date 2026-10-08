package com.scms.backend.scheduling;
import java.time.Instant; import java.util.Optional; import java.util.UUID; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
interface AttendanceRepository extends JpaRepository<Attendance,UUID>{ Optional<Attendance> findByBookingId(UUID bookingId);
	@Modifying
	@Query(value="""
		insert into attendance (id, booking_id, status, recording_source, recorded_at, updated_at, version)
		select gen_random_uuid(), booking.id, 'ABSENT', 'SYSTEM', session.start_time, session.start_time, 0
		from bookings booking
		join class_sessions session on session.id = booking.class_session_id
		where booking.status = 'BOOKED' and session.status <> 'CANCELLED' and session.start_time <= :now
		on conflict (booking_id) do nothing
		""",nativeQuery=true)
	int insertAbsentForStartedBookings(@Param("now") Instant now); }
