package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Query(value = "select attendance.id as \"attendanceId\", booking.id as \"bookingId\", booking.member_account_id as \"memberId\", profile.member_code as \"memberCode\", account.full_name as \"fullName\" from bookings booking join accounts account on account.id=booking.member_account_id join member_profiles profile on profile.account_id=account.id left join attendance on attendance.booking_id=booking.id where booking.class_session_id=:sessionId and booking.status='BOOKED' order by account.full_name, account.id", nativeQuery=true)
	List<AttendanceMemberView> findBookedAttendanceMembers(@Param("sessionId") UUID sessionId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select booking from Booking booking where booking.id = :id")
	Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

	@Query(value = """
		with required_benefits as (
			select 'CLASS_BOOKING'::varchar(50) as benefit_code
			union
			select required_benefit.benefit_code
			from class_sessions session
			join sport_classes sport_class on sport_class.id = session.sport_class_id
			join discipline_required_benefits required_benefit
			  on required_benefit.discipline_id = sport_class.discipline_id
			where session.id = :sessionId
		), covering_memberships as (
			select membership.id, membership.plan_code_snapshot, membership.ends_at
			from memberships membership
			where membership.member_account_id = :memberId
			  and membership.status = 'ACTIVE'
			  and membership.starts_at <= :sessionStart
			  and membership.ends_at > :sessionStart
		)
		select membership.id as "membershipId",
			required_benefit.benefit_code as "requiredBenefitCode",
			(provided_benefit.benefit_code is not null) as "provided"
		from required_benefits required_benefit
		left join covering_memberships membership on true
		left join membership_plan_benefits provided_benefit
		  on provided_benefit.plan_code = membership.plan_code_snapshot
		 and provided_benefit.benefit_code = required_benefit.benefit_code
		order by membership.ends_at desc nulls last, membership.id nulls last,
			required_benefit.benefit_code
		""", nativeQuery = true)
	List<BookingBenefitEligibilityView> findBookingBenefitEligibility(@Param("memberId") UUID memberId,
		@Param("sessionId") UUID sessionId,
		@Param("sessionStart") Instant sessionStart);

	Page<Booking> findByMemberAccountId(UUID memberAccountId, Pageable pageable);

	Page<Booking> findByMemberAccountIdAndStatus(UUID memberAccountId, BookingStatus status, Pageable pageable);

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
	interface AttendanceMemberView { UUID getAttendanceId(); UUID getBookingId(); UUID getMemberId(); String getMemberCode(); String getFullName(); }

	interface BookingBenefitEligibilityView {
		UUID getMembershipId();
		String getRequiredBenefitCode();
		boolean getProvided();
	}
}
