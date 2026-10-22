package com.scms.backend.membership;

import java.util.UUID;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Membership m where m.id = :id")
	Optional<Membership> findByIdForUpdate(@Param("id") UUID id);

	boolean existsByMemberAccountIdAndStatus(UUID memberAccountId, MembershipStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Membership m where m.memberAccountId = :memberId and m.status = :status")
	Optional<Membership> findByMemberAccountIdAndStatusForUpdate(@Param("memberId") UUID memberId,
		@Param("status") MembershipStatus status);

	@Query(value = "select * from memberships where member_account_id = :memberId and status = 'EXPIRED' "
		+ "and ends_at <= :now order by ends_at desc limit 1 for update", nativeQuery = true)
	Optional<Membership> findLatestExpiredByMemberAccountIdForUpdate(@Param("memberId") UUID memberId,
		@Param("now") Instant now);

	@Query(value = """
		select distinct membership.member_account_id
		from memberships membership
		where (cast(:afterMemberId as uuid) is null or membership.member_account_id > cast(:afterMemberId as uuid))
		  and ((membership.status = 'ACTIVE' and membership.ends_at <= :now)
		   or (membership.status = 'RESERVED' and exists (
			select 1 from memberships expired
			where expired.member_account_id = membership.member_account_id
			  and expired.status = 'EXPIRED'
			  and expired.ends_at <= :now
		   ) and not exists (
			select 1 from memberships active
			where active.member_account_id = membership.member_account_id
			  and active.status = 'ACTIVE'
		   )))
		order by membership.member_account_id
		limit 100
		""",
		nativeQuery = true)
	List<UUID> findDueMemberAccountIds(@Param("now") Instant now, @Param("afterMemberId") UUID afterMemberId);

}
