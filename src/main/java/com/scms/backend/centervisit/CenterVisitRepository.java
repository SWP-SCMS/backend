package com.scms.backend.centervisit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CenterVisitRepository extends JpaRepository<CenterVisit, UUID> {

	interface CourseAdmission {
		UUID getSessionId();
		String getCourseName();
		Instant getStartsAt();
		boolean getEligible();
		boolean getToday();
	}

	@Query(value = """
		select s.id as "sessionId", c.name as "courseName", s.start_at as "startsAt",
			(:at >= s.start_at - interval '30 minutes' and :at < s.end_at + interval '30 minutes') as eligible,
			((s.start_at at time zone 'Asia/Ho_Chi_Minh')::date =
			 (cast(:at as timestamptz) at time zone 'Asia/Ho_Chi_Minh')::date) as today
		from course_enrollments e join course_runs r on r.id=e.course_run_id
		join courses c on c.id=r.course_id
		join course_enrollment_sessions es on es.course_enrollment_id=e.id
		join course_sessions s on s.id=es.course_session_id and s.course_run_id=r.id
		where e.member_account_id=:memberId and e.lifecycle_status='ACTIVE'
		and es.disposition='ENROLLED' and r.status <> 'CANCELLED'
		and s.status in ('SCHEDULED','MAKEUP_SCHEDULED','COMPLETED')
		order by eligible desc, today desc, (s.end_at + interval '30 minutes' > :at) desc,
			abs(extract(epoch from s.start_at - cast(:at as timestamptz))), s.id limit 1
		""", nativeQuery = true)
	Optional<CourseAdmission> findCourseAdmission(@Param("memberId") UUID memberId, @Param("at") Instant at);

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
		with eligible_run as materialized (
			select run.id from course_runs run
			where run.id = (select course_run_id from course_sessions where id = :sessionId)
			and run.status <> 'CANCELLED'
			for update of run
		)
		select e.id from course_enrollment_sessions es
		join course_enrollments e on e.id = es.course_enrollment_id
		join course_sessions s on s.id = es.course_session_id
		join eligible_run run on run.id = e.course_run_id
		where e.member_account_id = :memberId
		and s.id = :sessionId
		and es.disposition = 'ENROLLED'
		and e.lifecycle_status = 'ACTIVE'
		and s.status in ('SCHEDULED', 'MAKEUP_SCHEDULED', 'COMPLETED')
		and :at >= s.start_at - interval '30 minutes'
		and :at < s.end_at + interval '30 minutes'
		limit 1
		for update of e, s
		""", nativeQuery = true)
	Optional<UUID> findEligibleCourseEnrollmentId(@Param("memberId") UUID memberId, @Param("sessionId") UUID sessionId, @Param("at") Instant at);

	@Query(value = """
		select s.id from course_enrollment_sessions es
		join course_enrollments e on e.id = es.course_enrollment_id
		join course_sessions s on s.id = es.course_session_id
		join course_runs run on run.id = e.course_run_id and run.id = s.course_run_id
		where e.member_account_id = :memberId and e.lifecycle_status = 'ACTIVE'
		and es.disposition = 'ENROLLED' and run.status <> 'CANCELLED'
		and s.status in ('SCHEDULED', 'MAKEUP_SCHEDULED', 'COMPLETED')
		and :at >= s.start_at - interval '30 minutes' and :at < s.end_at + interval '30 minutes'
		order by s.start_at, s.id limit 1
		""", nativeQuery = true)
	Optional<UUID> findAdmissionSession(@Param("memberId") UUID memberId, @Param("at") Instant at);

	@Query(value = """
		select exists (select 1 from membership_freeze_periods freeze_period join memberships membership on membership.id = freeze_period.membership_id
		where membership.member_account_id = :memberId and freeze_period.status in ('SCHEDULED', 'ACTIVE')
		and coalesce(freeze_period.actual_start_date, freeze_period.scheduled_start_date) <= (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date
		and coalesce(freeze_period.actual_end_date, freeze_period.scheduled_end_date) > (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date)
		""", nativeQuery = true)
	boolean hasActiveMembershipFreeze(@Param("memberId") UUID memberId);

	Optional<CenterVisit> findByCheckedInByAccountIdAndIdempotencyKey(UUID checkedInByAccountId, String idempotencyKey);

	Optional<CenterVisit> findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(UUID memberAccountId);

	Optional<CenterVisit> findFirstByMemberAccountIdAndCheckedOutAtIsNotNullOrderByCheckedOutAtDescIdDesc(UUID memberAccountId);

	Page<CenterVisit> findByCheckedOutAtIsNull(Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update center_visits
		set checked_out_at = :at,
			checked_out_by_account_id = :actorId,
			checkout_source = 'MEMBERSHIP_TERMINATION'
		where member_account_id = :memberId and checked_out_at is null and visit_source = 'MEMBERSHIP'
		""", nativeQuery = true)
	int checkoutOpenForMembershipTermination(@Param("memberId") UUID memberId, @Param("actorId") UUID actorId, @Param("at") Instant at);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update center_visits set checked_out_at = :at, checked_out_by_account_id = coalesce(:actorId, checked_in_by_account_id),
			checkout_source = 'MEMBERSHIP_FREEZE'
		where member_account_id = :memberId and checked_out_at is null and visit_source = 'MEMBERSHIP'
		""", nativeQuery = true)
	int checkoutOpenForMembershipFreeze(@Param("memberId") UUID memberId, @Param("actorId") UUID actorId, @Param("at") Instant at);

	@Query(value = """
		select id from center_visits
		where member_account_id = :memberId
		and checked_in_at <= :now
		and checked_in_at < :sessionEnd
		and (checked_out_at is null or checked_out_at > :sessionStart)
		order by checked_in_at desc, id
		limit 1
		""", nativeQuery = true)
	Optional<UUID> findOverlappingVisitId(@Param("memberId") UUID memberId, @Param("sessionStart") Instant sessionStart, @Param("sessionEnd") Instant sessionEnd, @Param("now") Instant now);
}
