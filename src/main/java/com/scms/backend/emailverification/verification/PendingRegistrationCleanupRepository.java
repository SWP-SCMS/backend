package com.scms.backend.emailverification.verification;

import com.scms.backend.emailverification.verification.PendingRegistrationCleanup.ManualRequestAudit;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PendingRegistrationCleanupRepository {

	List<UUID> cleanupFindAccounts(Timestamp nowTimestamp) {
		return db.query("select id from accounts where status='INACTIVE' and email_verification_required=true and registration_expires_at<=? order by registration_expires_at for update skip locked limit 200", (rs, n) -> rs.getObject(1, UUID.class), nowTimestamp);
	}

	List<ManualRequestAudit> cleanupFindEmailVerificationManualActivationRequests(UUID id) {
		return db.query("select id,requested_by_account_id,reason_code,verification_method from email_verification_manual_activation_requests where account_id=? and status='PENDING' for update", (rs, n) -> new ManualRequestAudit(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4)), id);
	}

	int cleanupUpdateEmailVerificationChallenges(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID id) {
		return db.update("update email_verification_challenges set superseded_at=coalesce(superseded_at,?),updated_at=?,version=version+1 where account_id=? and consumed_at is null", nowTimestamp, nowTimestamp2, id);
	}

	int cleanupUpdateEmailVerificationSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID id) {
		return db.update("update email_verification_sessions set revoked_at=coalesce(revoked_at,?),updated_at=? where account_id=?", nowTimestamp, nowTimestamp2, id);
	}

	int cleanupUpdateEmailOutbox(Timestamp nowTimestamp, UUID id) {
		return db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and status in ('PENDING','FAILED','PROCESSING')", nowTimestamp, id);
	}

	int cleanupUpdateRefreshTokens(Timestamp nowTimestamp, UUID id) {
		return db.update("update refresh_tokens set revoked_at=coalesce(revoked_at,?),version=version+1 where account_id=? and revoked_at is null", nowTimestamp, id);
	}

	int cleanupUpdateEmailVerificationManualActivationRequests(Timestamp nowTimestamp, UUID id) {
		return db.update("update email_verification_manual_activation_requests set status='EXPIRED',terminal_reason='REGISTRATION_EXPIRED',updated_at=?,version=version+1 where account_id=? and status='PENDING'", nowTimestamp, id);
	}

	int cleanupDeleteEmailVerificationRateLimits(Timestamp value1) {
		return db.update("delete from email_verification_rate_limits where expires_at<?", value1);
	}

	private final JdbcTemplate db;

	PendingRegistrationCleanupRepository(JdbcTemplate db) {
		this.db = db;
	}
}
