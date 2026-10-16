package com.scms.backend.notification;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class NotificationOutboxRepository {

	private final JdbcTemplate db;

	NotificationOutboxRepository(JdbcTemplate db) {
		this.db = db;
	}

	int enqueue(UUID id, String eventKey, UUID recipientId, String notificationType, String targetType, UUID targetId,
			String payload) {
		return db.update("""
			insert into notification_outbox(id,event_key,recipient_account_id,notification_type,target_type,target_id,payload)
			values(?,?,?,?,?,?,cast(? as jsonb))
			on conflict(event_key) do nothing
			""", id, eventKey, recipientId, notificationType, targetType, targetId, payload);
	}

	Optional<NotificationOutboxEvent> claimNext(Instant now, Instant leaseUntil) {
		List<NotificationOutboxEvent> claimed = db.query("""
			with candidate as (
				select id from notification_outbox
				where (status in ('PENDING','FAILED') and next_attempt_at <= ?)
				   or (status = 'PROCESSING' and next_attempt_at <= ?)
				order by created_at, id
				for update skip locked
				limit 1
			)
			update notification_outbox o
			set status='PROCESSING', attempt_count=o.attempt_count+1, last_attempt_at=?, next_attempt_at=?, updated_at=?
			from candidate c where o.id=c.id
			returning o.id,o.event_key,o.recipient_account_id,o.notification_type,o.target_type,o.target_id,
				o.payload::text,o.attempt_count
			""", (rs, rowNum) -> new NotificationOutboxEvent(rs.getObject("id", UUID.class),
				rs.getString("event_key"), rs.getObject("recipient_account_id", UUID.class),
				rs.getString("notification_type"), rs.getString("target_type"), rs.getObject("target_id", UUID.class),
				rs.getString("payload"), rs.getInt("attempt_count")), Timestamp.from(now), Timestamp.from(now),
			Timestamp.from(now), Timestamp.from(leaseUntil), Timestamp.from(now));
		return claimed.stream().findFirst();
	}

	void markSent(UUID id, Instant now) {
		db.update("""
			update notification_outbox
			set status='SENT', delivered_at=?, next_attempt_at=null, last_error=null, updated_at=?
			where id=?
			""", Timestamp.from(now), Timestamp.from(now), id);
	}

	void markFailed(UUID id, Instant nextAttemptAt, Instant now, String error) {
		db.update("""
			update notification_outbox
			set status='FAILED', next_attempt_at=?, last_error=?, updated_at=?
			where id=? and status<>'SENT'
			""", Timestamp.from(nextAttemptAt), error, Timestamp.from(now), id);
	}

	void markTerminalFailed(UUID id, Instant now, String error) {
		db.update("""
			update notification_outbox
			set status='TERMINAL_FAILED', next_attempt_at=null, last_error=?, updated_at=?
			where id=? and status<>'SENT'
			""", error, Timestamp.from(now), id);
	}
}
