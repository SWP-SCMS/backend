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

	boolean existsByMemberAccountIdAndStatus(UUID memberAccountId, MembershipStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Membership m where m.memberAccountId = :memberId and m.status = :status")
	Optional<Membership> findByMemberAccountIdAndStatusForUpdate(@Param("memberId") UUID memberId,
		@Param("status") MembershipStatus status);

	@Query(value = "select * from memberships where member_account_id = :memberId and status = 'EXPIRED' "
		+ "and ends_at <= :now order by ends_at desc limit 1 for update", nativeQuery = true)
	Optional<Membership> findLatestExpiredByMemberAccountIdForUpdate(@Param("memberId") UUID memberId,
		@Param("now") Instant now);

	@Query(value = "select distinct member_account_id from memberships where status='ACTIVE' and ends_at <= :now order by member_account_id limit 100",
		nativeQuery = true)
	List<UUID> findDueMemberAccountIds(@Param("now") Instant now);

}
