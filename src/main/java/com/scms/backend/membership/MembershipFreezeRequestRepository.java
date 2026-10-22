package com.scms.backend.membership;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipFreezeRequestRepository extends JpaRepository<MembershipFreezeRequest, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from MembershipFreezeRequest request where request.id = :id")
	Optional<MembershipFreezeRequest> findByIdForUpdate(@Param("id") UUID id);
	Page<MembershipFreezeRequest> findByMemberAccountIdOrderByCreatedAtDesc(UUID memberAccountId, Pageable pageable);
	Page<MembershipFreezeRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
	boolean existsByMembershipIdAndStatusIn(UUID membershipId, Collection<MembershipFreezeRequestStatus> statuses);
}
