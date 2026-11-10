package com.scms.backend.emailverification.passwordreset;

import com.scms.backend.emailverification.passwordreset.PasswordResetService.ActiveRequest;
import com.scms.backend.emailverification.passwordreset.PasswordResetService.RequestRow;
import com.scms.backend.emailverification.passwordreset.PasswordResetService.SessionRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PasswordResetRepository {

	java.util.List<ActiveRequest> requestFindPasswordResetRequests(UUID accountId) {
		return db.query("""
			select id,status,generation,resend_available_at from password_reset_requests
			where account_id=? and status in ('PENDING','VERIFIED') for update
			""", (rs, n) -> new ActiveRequest(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3), rs.getTimestamp(4).toInstant()), accountId);
	}

	int requestUpdatePasswordResetRequests(Timestamp nowTimestamp, UUID activeId) {
		return db.update("update password_reset_requests set status='SUPERSEDED',updated_at=?,version=version+1 where id=?", nowTimestamp, activeId);
	}

	int requestUpdatePasswordResetSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID activeId) {
		return db.update("update password_reset_sessions set revoked_at=?,updated_at=? where request_id=? and used_at is null and revoked_at is null", nowTimestamp, nowTimestamp2, activeId);
	}

	int requestInsertPasswordResetRequests(UUID requestId, UUID accountId, int generation, java.lang.String digest, java.lang.String hmacVersion, Timestamp expiresAtTimestamp, int maxAttempts, Timestamp resendAtTimestamp) {
		return db.update("""
			insert into password_reset_requests(id,account_id,status,generation,otp_digest,hmac_key_version,
			 expires_at,max_attempts,resend_available_at) values (?,?,'PENDING',?,?,?,?,?,?)
			""", requestId, accountId, generation, digest, hmacVersion, expiresAtTimestamp, maxAttempts, resendAtTimestamp);
	}

	int regenerateResetChallenge(int generation, java.lang.String digest, java.lang.String hmacVersion, Timestamp expiresAtTimestamp, int maxAttempts, Timestamp resendAtTimestamp, Timestamp nowTimestamp, UUID requestId) {
		return db.update("""
			update password_reset_requests set generation=?,otp_digest=?,hmac_key_version=?,expires_at=?,
			 attempt_count=0,max_attempts=?,resend_available_at=?,updated_at=?,version=version+1 where id=?
			""", generation, digest, hmacVersion, expiresAtTimestamp, maxAttempts, resendAtTimestamp, nowTimestamp, requestId);
	}

	java.util.List<RequestRow> verifyFindPasswordResetRequests(java.lang.String email) {
		return db.query("""
			select r.id,r.account_id,r.status,r.generation,r.otp_digest,r.hmac_key_version,r.expires_at,
			 r.attempt_count,r.max_attempts from password_reset_requests r join accounts a on a.id=r.account_id
			where lower(a.email)=lower(?) and a.status='ACTIVE' and r.status='PENDING'
			order by r.created_at desc limit 1 for update of r,a
			""", this::requestRow, email);
	}

	int verifyUpdatePasswordResetRequests(int attempts, java.lang.String status, Timestamp nowTimestamp, UUID rowId) {
		return db.update("update password_reset_requests set attempt_count=?,status=?,updated_at=?,version=version+1 where id=?", attempts, status, nowTimestamp, rowId);
	}

	int markResetRequestVerified(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID rowId) {
		return db.update("update password_reset_requests set status='VERIFIED',verified_at=?,updated_at=?,version=version+1 where id=?", nowTimestamp, nowTimestamp2, rowId);
	}

	int verifyUpdatePasswordResetSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID accountId) {
		return db.update("update password_reset_sessions set revoked_at=?,updated_at=? where account_id=? and used_at is null and revoked_at is null", nowTimestamp, nowTimestamp2, accountId);
	}

	int verifyInsertPasswordResetSessions(UUID randomUUID, UUID rowId, UUID accountId, java.lang.String value4, Timestamp sessionExpiresAtTimestamp) {
		return db.update("insert into password_reset_sessions(id,request_id,account_id,token_hash,expires_at) values (?,?,?,?,?)", randomUUID, rowId, accountId, value4, sessionExpiresAtTimestamp);
	}

	java.util.List<SessionRow> confirmFindPasswordResetSessions(java.lang.String value2) {
		return db.query("""
			select s.id,s.request_id,s.account_id,s.expires_at,s.used_at,s.revoked_at,r.status
			from password_reset_sessions s join password_reset_requests r on r.id=s.request_id
			where s.token_hash=? for update of s,r
			""", this::sessionRow, value2);
	}

	int confirmUpdatePasswordResetSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID sessionId) {
		return db.update("update password_reset_sessions set used_at=?,updated_at=? where id=?", nowTimestamp, nowTimestamp2, sessionId);
	}

	int confirmUpdatePasswordResetRequests(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID requestId) {
		return db.update("update password_reset_requests set status='COMPLETED',completed_at=?,updated_at=?,version=version+1 where id=?", nowTimestamp, nowTimestamp2, requestId);
	}

	int revokeOtherResetSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID accountId, UUID sessionId) {
		return db.update("update password_reset_sessions set revoked_at=?,updated_at=? where account_id=? and id<>? and used_at is null and revoked_at is null", nowTimestamp, nowTimestamp2, accountId, sessionId);
	}

	int cancelOtpOutboxUpdateEmailOutbox(Timestamp nowTimestamp, java.lang.String value2) {
		return db.update("""
			update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,
			 locked_by=null,updated_at=? where event_key like ? and status in ('PENDING','FAILED','PROCESSING')
			""", nowTimestamp, value2);
	}

	int expireUpdatePasswordResetRequests(Timestamp nowTimestamp, UUID rowId) {
		return db.update("update password_reset_requests set status='EXPIRED',updated_at=?,version=version+1 where id=? and status='PENDING'", nowTimestamp, rowId);
	}

	private RequestRow requestRow(ResultSet rs, int n) throws SQLException {
		return new RequestRow(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant(), rs.getInt(8), rs.getInt(9));
	}

	private SessionRow sessionRow(ResultSet rs, int n) throws SQLException {
		return new SessionRow(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getTimestamp(4).toInstant(), instant(rs, 5), instant(rs, 6), rs.getString(7));
	}

	private Instant instant(ResultSet rs, int index) throws SQLException {
		Timestamp value = rs.getTimestamp(index);
		return value == null ? null : value.toInstant();
	}

	private final JdbcTemplate db;

	PasswordResetRepository(JdbcTemplate db) {
		this.db = db;
	}
}
