package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@Query(value = """
		select account.id as "memberId", profile.member_code as "memberCode",
			account.full_name as "fullName", account.phone as "phone",
			profile.profile_image_url as "profileImageUrl"
		from bookings booking
		join accounts account on account.id = booking.member_account_id
		join member_profiles profile on profile.account_id = account.id
		where booking.class_session_id = :sessionId and booking.status = 'BOOKED'
		order by account.full_name, account.id
		""", nativeQuery = true)
	List<BookedMemberView> findBookedMembersBySessionId(@Param("sessionId") UUID sessionId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select booking from Booking booking where booking.id = :id")
	Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

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

	interface BookedMemberView {
		UUID getMemberId();
		String getMemberCode();
		String getFullName();
		String getPhone();
		String getProfileImageUrl();
	}
}
