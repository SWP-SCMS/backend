package com.scms.backend.notification;

import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	@Modifying
	@Query(value = """
		insert into notifications (id, event_key, recipient_account_id, notification_type, target_type, target_id,
			payload, delivery_status, delivery_attempt_count)
		values (:id, :eventKey, :recipientId, :notificationType, :targetType, :targetId,
			cast(:payload as jsonb), 'PENDING', 0)
		on conflict (event_key) do nothing
		""", nativeQuery = true)
	int insertPending(@Param("id") UUID id, @Param("eventKey") String eventKey,
			@Param("recipientId") UUID recipientId, @Param("notificationType") String notificationType,
			@Param("targetType") String targetType, @Param("targetId") UUID targetId,
			@Param("payload") String payload);
}
