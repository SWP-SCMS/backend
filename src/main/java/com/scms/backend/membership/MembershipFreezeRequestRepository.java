package com.scms.backend.membership;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

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

	@Query(value = """
		select id from membership_freeze_requests
		where status = 'APPROVED_PENDING_PAYMENT' and start_date <= :businessDate
		order by start_date, id limit 100
		""", nativeQuery = true)
	List<UUID> findDueUnpaidIds(@Param("businessDate") LocalDate businessDate);

	@Query(value = """
		select request.id from membership_freeze_requests request
		join membership_freeze_periods period on period.request_id = request.id
		where request.status = 'SCHEDULED' and period.status = 'SCHEDULED'
		and period.scheduled_start_date <= :businessDate
		order by period.scheduled_start_date, request.id limit 100
		""", nativeQuery = true)
	List<UUID> findDueScheduledIds(@Param("businessDate") LocalDate businessDate);

	@Query(value = """
		select request.id from membership_freeze_requests request
		join membership_freeze_periods period on period.request_id = request.id
		where request.status = 'ACTIVE' and period.status = 'ACTIVE'
		and period.scheduled_end_date <= :businessDate
		order by period.scheduled_end_date, request.id limit 100
		""", nativeQuery = true)
	List<UUID> findDueActiveIds(@Param("businessDate") LocalDate businessDate);

	@Query(value = """
		select request.id from membership_freeze_requests request
		where request.member_account_id = :memberAccountId and request.status = 'ACTIVE'
		order by request.created_at, request.id
		""", nativeQuery = true)
	List<UUID> findActiveIdsByMemberAccountId(@Param("memberAccountId") UUID memberAccountId);
}
