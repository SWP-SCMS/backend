package com.scms.backend.centervisit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CenterVisitRepository extends JpaRepository<CenterVisit, UUID> {

	@Query(value = """
		select m.id from memberships m
		join membership_plan_benefits mpb on mpb.plan_code = m.plan_code_snapshot
		where m.member_account_id = :memberId
		and m.status = 'ACTIVE'
		and mpb.benefit_code = 'CENTER_ACCESS'
		and m.starts_at <= :at and m.ends_at > :at
		order by m.ends_at desc, m.id
		limit 1
		""", nativeQuery = true)
	Optional<UUID> findCurrentMembership(@Param("memberId") UUID memberId, @Param("at") Instant at);

	@Query(value = """
		select e.id from course_enrollment_sessions es
		join course_enrollments e on e.id = es.course_enrollment_id
		join course_sessions s on s.id = es.course_session_id
		where e.member_account_id = :memberId
		and s.id = :sessionId
		and es.disposition = 'ENROLLED'
		and s.status in ('SCHEDULED', 'MAKEUP_SCHEDULED', 'COMPLETED')
		and :at >= s.start_at - interval '30 minutes'
		and :at < s.end_at + interval '30 minutes'
		limit 1
		for update of e, s
		""", nativeQuery = true)
	Optional<UUID> findEligibleCourseEnrollmentId(@Param("memberId") UUID memberId,
		@Param("sessionId") UUID sessionId, @Param("at") Instant at);

	@Query(value = """
		select exists (select 1 from membership_freeze_periods freeze_period join memberships membership on membership.id = freeze_period.membership_id
		where membership.member_account_id = :memberId and freeze_period.status in ('SCHEDULED', 'ACTIVE')
		and coalesce(freeze_period.actual_start_date, freeze_period.scheduled_start_date) <= (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date
		and coalesce(freeze_period.actual_end_date, freeze_period.scheduled_end_date) > (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date)
		""", nativeQuery = true)
	boolean hasActiveMembershipFreeze(@Param("memberId") UUID memberId);

	Optional<CenterVisit> findByCheckedInByAccountIdAndIdempotencyKey(UUID checkedInByAccountId,
		String idempotencyKey);

	Optional<CenterVisit> findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(
		UUID memberAccountId);

	Optional<CenterVisit> findFirstByMemberAccountIdAndCheckedOutAtIsNotNullOrderByCheckedOutAtDescIdDesc(
		UUID memberAccountId);

	Page<CenterVisit> findByCheckedOutAtIsNull(Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update center_visits
		set checked_out_at = :at,
			checked_out_by_account_id = :actorId,
			checkout_source = 'MEMBERSHIP_TERMINATION'
		where member_account_id = :memberId and checked_out_at is null and visit_source = 'MEMBERSHIP'
		""", nativeQuery = true)
	int checkoutOpenForMembershipTermination(@Param("memberId") UUID memberId,
		@Param("actorId") UUID actorId, @Param("at") Instant at);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update center_visits set checked_out_at = :at, checked_out_by_account_id = coalesce(:actorId, checked_in_by_account_id),
			checkout_source = 'MEMBERSHIP_FREEZE'
		where member_account_id = :memberId and checked_out_at is null and visit_source = 'MEMBERSHIP'
		""", nativeQuery = true)
	int checkoutOpenForMembershipFreeze(@Param("memberId") UUID memberId, @Param("actorId") UUID actorId,
		@Param("at") Instant at);

	@Query(value = """
		select id from center_visits
		where member_account_id = :memberId
		and visit_source = 'MEMBERSHIP'
		and checked_in_at <= :now
		and checked_in_at < :sessionEnd
		and (checked_out_at is null or checked_out_at > :sessionStart)
		order by checked_in_at desc, id
		limit 1
		""", nativeQuery = true)
	Optional<UUID> findOverlappingVisitId(@Param("memberId") UUID memberId,
		@Param("sessionStart") Instant sessionStart, @Param("sessionEnd") Instant sessionEnd,
		@Param("now") Instant now);
}
