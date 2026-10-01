package com.scms.backend.membership;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	boolean existsByMemberAccountIdAndStatus(UUID memberAccountId, MembershipStatus status);
}
