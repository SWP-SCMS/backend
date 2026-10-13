package com.scms.backend.training;

import java.time.Instant;
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

	Page<CoachAssignment> findByMemberAccountIdOrderByCreatedAtDesc(UUID memberAccountId, Pageable pageable);

	boolean existsByCoachAccountIdAndStatus(UUID coachAccountId, CoachAssignmentStatus status);

	Page<CoachAssignment> findByCoachAccountIdAndStatusOrderByCreatedAtDesc(UUID coachAccountId,
			CoachAssignmentStatus status, Pageable pageable);

	long countByCoachAccountIdAndStatus(UUID coachAccountId, CoachAssignmentStatus status);

	@Query(value = """
		select exists (
			select 1 from memberships
			where member_account_id = :memberId
			  and status = 'ACTIVE'
			  and plan_code_snapshot = 'PLUS'
			  and starts_at <= :at
			  and ends_at > :at
		)
		""", nativeQuery = true)
	boolean hasActivePlusMembership(@Param("memberId") UUID memberId, @Param("at") Instant at);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update coach_assignments ca
		set status = 'ENDED',
		    ends_at = greatest(
		        ca.starts_at,
		        coalesce(
		            (select max(m.ends_at) from memberships m where m.member_account_id = ca.member_account_id and m.plan_code_snapshot = 'PLUS'),
		            :now
		        )
		    ),
		    end_reason = 'PLUS membership expired',
		    updated_at = :now,
		    version = ca.version + 1
		where ca.member_account_id = :memberId
		  and ca.status = 'ACTIVE'
		  and not exists (
		      select 1 from memberships m
		      where m.member_account_id = ca.member_account_id
		        and m.status = 'ACTIVE'
		        and m.plan_code_snapshot = 'PLUS'
		        and m.starts_at <= :now
		        and m.ends_at > :now
		  )
		""", nativeQuery = true)
	int endActiveAssignmentIfPlusExpired(@Param("memberId") UUID memberId, @Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		update coach_assignments ca
		set status = 'ENDED',
		    ends_at = greatest(
		        ca.starts_at,
		        coalesce(
		            (select max(m.ends_at) from memberships m where m.member_account_id = ca.member_account_id and m.plan_code_snapshot = 'PLUS'),
		            :now
		        )
		    ),
		    end_reason = 'PLUS membership expired',
		    updated_at = :now,
		    version = ca.version + 1
		where ca.status = 'ACTIVE'
		  and not exists (
		      select 1 from memberships m
		      where m.member_account_id = ca.member_account_id
		        and m.status = 'ACTIVE'
		        and m.plan_code_snapshot = 'PLUS'
		        and m.starts_at <= :now
		        and m.ends_at > :now
		  )
		""", nativeQuery = true)
	int endAllActiveAssignmentsWithExpiredPlus(@Param("now") Instant now);
}
