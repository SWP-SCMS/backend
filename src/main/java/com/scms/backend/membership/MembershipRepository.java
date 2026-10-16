package com.scms.backend.membership;

import java.util.UUID;
import java.time.Instant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	boolean existsByMemberAccountIdAndStatus(UUID memberAccountId, MembershipStatus status);

	@Modifying
	@Query(value = "update memberships set status = 'EXPIRED' where status = 'ACTIVE' and ends_at <= :now",
		nativeQuery = true)
	int expireDue(@Param("now") Instant now);
}
