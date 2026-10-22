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
		where member_account_id = :memberId and checked_out_at is null
		""", nativeQuery = true)
	int checkoutOpenForMembershipTermination(@Param("memberId") UUID memberId,
		@Param("actorId") UUID actorId, @Param("at") Instant at);

	@Query(value = """
		select id from center_visits
		where member_account_id = :memberId
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
