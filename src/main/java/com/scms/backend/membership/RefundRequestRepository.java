package com.scms.backend.membership;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from RefundRequest request where request.id = :id")
	Optional<RefundRequest> findByIdForUpdate(@Param("id") UUID id);
	Optional<RefundRequest> findByRequesterAccountIdAndIdempotencyKey(UUID requesterAccountId, String idempotencyKey);
	Page<RefundRequest> findAllByOrderByRequestedAtDescIdDesc(Pageable pageable);
}
