package com.scms.backend.membership;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipOrderRepository extends JpaRepository<MembershipOrder, UUID> {

	boolean existsByMemberAccountIdAndStatus(UUID memberAccountId, MembershipOrderStatus status);

	Optional<MembershipOrder> findByMemberAccountIdAndStatus(UUID memberAccountId,
		MembershipOrderStatus status);

	@Query(value = "select exists (select 1 from membership_orders where plan_code_snapshot = :planCode)", nativeQuery = true)
	boolean existsByPlanCodeSnapshot(@Param("planCode") String planCode);

	@Query("select distinct membershipOrder.planCodeSnapshot from MembershipOrder membershipOrder "
		+ "where membershipOrder.planCodeSnapshot in :planCodes")
	Set<String> findUsedPlanCodes(@Param("planCodes") Set<String> planCodes);
}
