package com.scms.backend.membership;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface MembershipTerminationRequestRepository extends JpaRepository<MembershipTerminationRequest, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from MembershipTerminationRequest request where request.id = :id")
	java.util.Optional<MembershipTerminationRequest> findByIdForUpdate(@Param("id") UUID id);

	Page<MembershipTerminationRequest> findByMemberAccountIdOrderByCreatedAtDesc(UUID memberAccountId, Pageable pageable);
	Page<MembershipTerminationRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
	boolean existsByMembershipIdAndStatusIn(UUID membershipId,
		java.util.Collection<MembershipTerminationRequestStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from MembershipTerminationRequest request where request.status = com.scms.backend.membership.MembershipTerminationRequestStatus.APPROVED "
		+ "and request.effectiveAt <= :now order by request.effectiveAt, request.id")
	java.util.List<MembershipTerminationRequest> findApprovedDueForUpdate(@Param("now") java.time.Instant now);
}
