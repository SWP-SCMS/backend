package com.scms.backend.reception;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CenterVisitRepository extends JpaRepository<CenterVisit, UUID> {

	@Query(value = """
		select id from memberships
		where member_account_id = :memberId
		and status = 'ACTIVE'
		and plan_code_snapshot in ('BASIC', 'PLUS')
		and starts_at <= :at and ends_at > :at
		order by ends_at desc, id
		limit 1
		""", nativeQuery = true)
	Optional<UUID> findCurrentMembership(@Param("memberId") UUID memberId, @Param("at") Instant at);
}
