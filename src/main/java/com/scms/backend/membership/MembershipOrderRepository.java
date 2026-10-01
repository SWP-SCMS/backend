package com.scms.backend.membership;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipOrderRepository extends JpaRepository<MembershipOrder, UUID> {

	boolean existsByMemberAccountIdAndStatus(UUID memberAccountId, MembershipOrderStatus status);

	Optional<MembershipOrder> findByMemberAccountIdAndStatus(UUID memberAccountId,
		MembershipOrderStatus status);
}
