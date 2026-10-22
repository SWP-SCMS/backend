package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SessionWaitlistRepository extends JpaRepository<SessionWaitlist, UUID> {
	Optional<SessionWaitlist> findBySessionIdAndMemberIdAndStatus(UUID sessionId, UUID memberId, WaitlistStatus status);
	List<SessionWaitlist> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
	List<SessionWaitlist> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select entry from SessionWaitlist entry where entry.sessionId = :sessionId and entry.memberId = :memberId and entry.status = com.scms.backend.scheduling.WaitlistStatus.WAITING")
	Optional<SessionWaitlist> findWaitingForUpdate(@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select entry from SessionWaitlist entry where entry.sessionId = :sessionId and entry.status = com.scms.backend.scheduling.WaitlistStatus.WAITING order by entry.createdAt, entry.id")
	List<SessionWaitlist> findWaitingBySessionForUpdate(@Param("sessionId") UUID sessionId);

	@Query(value = """
		select count(*) from session_waitlists entry
		where entry.class_session_id = :sessionId and entry.status = 'WAITING'
		  and (entry.created_at < :createdAt or (entry.created_at = :createdAt and entry.id < :id))
		""", nativeQuery = true)
	long countAhead(@Param("sessionId") UUID sessionId, @Param("createdAt") Instant createdAt, @Param("id") UUID id);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select entry from SessionWaitlist entry, ClassSession session
		where entry.sessionId = session.id
		  and entry.status = com.scms.backend.scheduling.WaitlistStatus.WAITING
		  and session.startTime <= :now
		order by entry.sessionId, entry.createdAt, entry.id
		""")
	List<SessionWaitlist> findExpiredForUpdate(@Param("now") Instant now);
}
