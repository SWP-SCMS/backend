package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@Query(value = """
		select id from memberships
		where member_account_id = :memberId and status = 'ACTIVE' and plan_code_snapshot = 'PLUS'
		  and starts_at <= :sessionStart and ends_at > :sessionStart
		order by ends_at desc limit 1
		""", nativeQuery = true)
	Optional<UUID> findEligiblePlusMembership(@Param("memberId") UUID memberId,
		@Param("sessionStart") Instant sessionStart);

	boolean existsByMemberAccountIdAndClassSessionIdAndStatus(UUID memberAccountId, UUID classSessionId,
		BookingStatus status);

	long countByClassSessionIdAndStatus(UUID classSessionId, BookingStatus status);

	@Query(value = """
		select exists (
			select 1 from bookings booking
			join class_sessions session on session.id = booking.class_session_id
			where booking.member_account_id = :memberId and booking.status = 'BOOKED'
			  and session.start_time < :endTime and session.end_time > :startTime
		)
		""", nativeQuery = true)
	boolean existsBookedOverlap(@Param("memberId") UUID memberId, @Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);
}
