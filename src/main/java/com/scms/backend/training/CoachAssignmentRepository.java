package com.scms.backend.training;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoachAssignmentRepository extends JpaRepository<CoachAssignment, UUID> {

	Optional<CoachAssignment> findByMemberAccountIdAndStatus(UUID memberAccountId, CoachAssignmentStatus status);

	Optional<CoachAssignment> findByMemberAccountIdAndCoachAccountIdAndStatus(UUID memberAccountId,
			UUID coachAccountId, CoachAssignmentStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from CoachAssignment a where a.memberAccountId = :memberAccountId and a.status = :status")
	Optional<CoachAssignment> findByMemberAccountIdAndStatusForUpdate(@Param("memberAccountId") UUID memberAccountId,
			@Param("status") CoachAssignmentStatus status);

	@Query("select a from CoachAssignment a where a.memberAccountId = :memberAccountId "
			+ "order by a.createdAt desc, a.id desc")
	Page<CoachAssignment> findByMemberAccountIdOrderByCreatedAtDesc(@Param("memberAccountId") UUID memberAccountId,
			Pageable pageable);

	boolean existsByCoachAccountIdAndStatus(UUID coachAccountId, CoachAssignmentStatus status);

	@Query("select a from CoachAssignment a where a.coachAccountId = :coachAccountId "
			+ "and a.status = :status order by a.createdAt desc, a.id desc")
	Page<CoachAssignment> findByCoachAccountIdAndStatusOrderByCreatedAtDesc(@Param("coachAccountId") UUID coachAccountId,
			@Param("status") CoachAssignmentStatus status, Pageable pageable);

	long countByCoachAccountIdAndStatus(UUID coachAccountId, CoachAssignmentStatus status);

	@Query(value = """
		select exists (
			select 1
			from memberships membership
			join membership_plan_benefits benefit
			  on benefit.plan_code = membership.plan_code_snapshot
			where membership.member_account_id = :memberId
			  and membership.status = 'ACTIVE'
			  and benefit.benefit_code = :benefitCode
			  and membership.starts_at <= :at
			  and membership.ends_at > :at
		)
		""", nativeQuery = true)
	boolean hasActiveMembershipBenefit(@Param("memberId") UUID memberId,
			@Param("benefitCode") String benefitCode, @Param("at") Instant at);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update coach_assignments ca
		set status = 'ENDED',
		    ends_at = greatest(
		        ca.starts_at,
		        coalesce(
		            (select max(m.ends_at)
		             from memberships m
		             join membership_plan_benefits benefit on benefit.plan_code = m.plan_code_snapshot
		             where m.member_account_id = ca.member_account_id
		               and benefit.benefit_code = 'PERSONAL_COACHING'
		               and m.starts_at <= :now
		               and m.ends_at <= :now),
		            :now
		        )
		    ),
		    end_reason = 'PERSONAL_COACHING Benefit expired',
		    updated_at = :now,
		    version = ca.version + 1
		where ca.member_account_id = :memberId
		  and ca.status = 'ACTIVE'
		  and not exists (
		      select 1
		      from memberships m
		      join membership_plan_benefits benefit on benefit.plan_code = m.plan_code_snapshot
		      where m.member_account_id = ca.member_account_id
		        and m.status = 'ACTIVE'
		        and benefit.benefit_code = 'PERSONAL_COACHING'
		        and m.starts_at <= :now
		        and m.ends_at > :now
		  )
		""", nativeQuery = true)
	int endActiveAssignmentIfPersonalCoachingExpired(@Param("memberId") UUID memberId,
			@Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update coach_assignments ca
		set status = 'ENDED',
		    ends_at = greatest(
		        ca.starts_at,
		        coalesce(
		            (select max(m.ends_at)
		             from memberships m
		             join membership_plan_benefits benefit on benefit.plan_code = m.plan_code_snapshot
		             where m.member_account_id = ca.member_account_id
		               and benefit.benefit_code = 'PERSONAL_COACHING'
		               and m.starts_at <= :now
		               and m.ends_at <= :now),
		            :now
		        )
		    ),
		    end_reason = 'PERSONAL_COACHING Benefit expired',
		    updated_at = :now,
		    version = ca.version + 1
		where ca.status = 'ACTIVE'
		  and not exists (
		      select 1
		      from memberships m
		      join membership_plan_benefits benefit on benefit.plan_code = m.plan_code_snapshot
		      where m.member_account_id = ca.member_account_id
		        and m.status = 'ACTIVE'
		        and benefit.benefit_code = 'PERSONAL_COACHING'
		        and m.starts_at <= :now
		        and m.ends_at > :now
		  )
		""", nativeQuery = true)
	int endAllActiveAssignmentsWithExpiredPersonalCoaching(@Param("now") Instant now);

	@Query(value = """
		select account.id as "memberAccountId", profile.member_code as "memberCode",
			account.full_name as "fullName", account.phone as "phone", account.email as "email",
			membership.ends_at as "membershipEndsAt",
			case when assignment.id is null then 'UNASSIGNED' else 'ASSIGNED' end as "assignmentStatus"
		from accounts account
		join member_profiles profile on profile.account_id = account.id
		join lateral (
			select memberships.ends_at
			from memberships
			join membership_plan_benefits benefit
			  on benefit.plan_code = memberships.plan_code_snapshot
			where memberships.member_account_id = account.id
			  and memberships.status = 'ACTIVE'
			  and benefit.benefit_code = 'PERSONAL_COACHING'
			  and memberships.starts_at <= :now
			  and memberships.ends_at > :now
			order by memberships.ends_at, memberships.id
			limit 1
		) membership on true
		left join coach_assignments assignment
		  on assignment.member_account_id = account.id and assignment.status = 'ACTIVE'
		where account.role = 'MEMBER' and account.status = 'ACTIVE'
		  and (cast(:query as text) is null or lower(profile.member_code) like lower(concat('%', cast(:query as text), '%'))
		    or lower(account.full_name) like lower(concat('%', cast(:query as text), '%'))
		    or account.phone like concat('%', cast(:query as text), '%')
		    or lower(account.email) like lower(concat('%', cast(:query as text), '%')))
		  and (:assignmentStatus = 'ALL'
		    or (:assignmentStatus = 'UNASSIGNED' and assignment.id is null)
		    or (:assignmentStatus = 'ASSIGNED' and assignment.id is not null))
		order by case when assignment.id is null then 0 else 1 end,
			membership.ends_at, profile.member_code, account.id
		""", countQuery = """
		select count(*)
		from accounts account
		join member_profiles profile on profile.account_id = account.id
		left join coach_assignments assignment
		  on assignment.member_account_id = account.id and assignment.status = 'ACTIVE'
		where account.role = 'MEMBER' and account.status = 'ACTIVE'
		  and exists (
			select 1
			from memberships
			join membership_plan_benefits benefit
			  on benefit.plan_code = memberships.plan_code_snapshot
			where memberships.member_account_id = account.id
			  and memberships.status = 'ACTIVE'
			  and benefit.benefit_code = 'PERSONAL_COACHING'
			  and memberships.starts_at <= :now
			  and memberships.ends_at > :now
		  )
		  and (cast(:query as text) is null or lower(profile.member_code) like lower(concat('%', cast(:query as text), '%'))
		    or lower(account.full_name) like lower(concat('%', cast(:query as text), '%'))
		    or account.phone like concat('%', cast(:query as text), '%')
		    or lower(account.email) like lower(concat('%', cast(:query as text), '%')))
		  and (:assignmentStatus = 'ALL'
		    or (:assignmentStatus = 'UNASSIGNED' and assignment.id is null)
		    or (:assignmentStatus = 'ASSIGNED' and assignment.id is not null))
		""", nativeQuery = true)
	Page<AssignmentCandidateView> findAssignmentCandidates(@Param("query") String query,
		@Param("assignmentStatus") String assignmentStatus, @Param("now") Instant now, Pageable pageable);

	@Query(value = """
		select account.id as "coachAccountId", account.full_name as "fullName",
			account.phone as "phone", account.email as "email", count(assignment.id) as "activeMemberCount"
		from accounts account
		left join coach_assignments assignment
		  on assignment.coach_account_id = account.id and assignment.status = 'ACTIVE'
		where account.role = 'COACH' and account.status = 'ACTIVE'
		group by account.id, account.full_name, account.phone, account.email
		order by account.full_name, account.id
		""", nativeQuery = true)
	List<CoachAssignmentWorkloadView> findAssignmentWorkloads();

	interface AssignmentCandidateView {
		UUID getMemberAccountId();
		String getMemberCode();
		String getFullName();
		String getPhone();
		String getEmail();
		Instant getMembershipEndsAt();
		String getAssignmentStatus();
	}

	interface CoachAssignmentWorkloadView {
		UUID getCoachAccountId();
		String getFullName();
		String getPhone();
		String getEmail();
		long getActiveMemberCount();
	}
}
