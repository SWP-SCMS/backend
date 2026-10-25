package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

class BookingCancellationRepositoryImpl implements BookingCancellationRepository {
	private final EntityManager entityManager;

	BookingCancellationRepositoryImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public List<UUID> cancelFutureBookingsForMembershipTermination(UUID memberId, UUID actorId, Instant now) {
		entityManager.flush();
		List<UUID> sessionIds = entityManager.createNativeQuery("""
			with cancelled as (
				update bookings booking
				set status = 'CANCELLED', cancelled_by_account_id = :actorId,
					cancellation_source = 'MEMBERSHIP_TERMINATED', cancelled_at = :now,
					updated_at = :now, version = version + 1
				where booking.member_account_id = :memberId and booking.status = 'BOOKED'
				  and exists (
					select 1 from class_sessions session
					where session.id = booking.class_session_id and session.start_time > :now
				  )
				returning booking.class_session_id
			)
			select distinct class_session_id from cancelled order by class_session_id
			""", UUID.class)
			.setParameter("memberId", memberId)
			.setParameter("actorId", actorId)
			.setParameter("now", now)
			.getResultList();
		entityManager.clear();
		return sessionIds;
	}

	@Override
	public List<UUID> cancelFutureBookingsForMembershipFreeze(UUID memberId, UUID actorId, Instant startAt,
			Instant endAt, Instant now) {
		entityManager.flush();
		List<UUID> sessionIds = entityManager.createNativeQuery("""
			with cancelled as (
				update bookings booking
				set status = 'CANCELLED', cancelled_by_account_id = :actorId,
					cancellation_source = 'MEMBERSHIP_FREEZE', cancelled_at = :now,
					updated_at = :now, version = version + 1
				where booking.member_account_id = :memberId and booking.status = 'BOOKED'
				  and exists (
					select 1 from class_sessions session
					where session.id = booking.class_session_id
					  and session.start_time >= :startAt and session.start_time < :endAt
					  and session.start_time > :now
				  )
				returning booking.class_session_id
			)
			select distinct class_session_id from cancelled order by class_session_id
			""", UUID.class)
			.setParameter("memberId", memberId)
			.setParameter("actorId", actorId)
			.setParameter("startAt", startAt)
			.setParameter("endAt", endAt)
			.setParameter("now", now)
			.getResultList();
		entityManager.clear();
		return sessionIds;
	}
}
