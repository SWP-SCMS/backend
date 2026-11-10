package com.scms.backend.emailverification.verification;

import com.scms.backend.emailverification.verification.EmailVerificationService.ChallengeRow;
import com.scms.backend.emailverification.verification.EmailVerificationService.ManualRequestAudit;
import com.scms.backend.emailverification.verification.EmailVerificationService.PendingTimes;
import com.scms.backend.emailverification.verification.EmailVerificationService.ResendRow;
import com.scms.backend.emailverification.verification.EmailVerificationService.SessionRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class EmailVerificationRepository {

	int beginInsertEmailVerificationSessions(UUID randomUUID, UUID accountId, java.lang.String value3, java.lang.String value4, Timestamp registrationExpiresAtTimestamp) {
		return db.update("""
			insert into email_verification_sessions(id,account_id,token_hash,csrf_token_hash,expires_at)
			values (?,?,?,?,?)
			""", randomUUID, accountId, value3, value4, registrationExpiresAtTimestamp);
	}

	java.util.List<PendingTimes> pendingConflictFindAccounts(java.lang.String value2, Timestamp nowTimestamp) {
		return db.query("""
			select c.expires_at,c.resend_available_at,a.registration_expires_at from accounts a
			join email_verification_challenges c on c.account_id=a.id and c.consumed_at is null and c.superseded_at is null
			where lower(a.email)=lower(?) and a.email_verification_required=true and a.registration_expires_at>?
			""", (rs, n) -> new PendingTimes(rs.getTimestamp(1).toInstant(), rs.getTimestamp(2).toInstant(), rs.getTimestamp(3).toInstant()), value2, nowTimestamp);
	}

	java.util.List<ChallengeRow> verifyFindAccounts(java.lang.String email) {
		return db.query("""
			select a.id,a.status,a.email_verification_required,a.registration_expires_at,a.email_verified_at,
			 c.id,c.otp_digest,c.hmac_key_version,c.expires_at,c.attempt_count,c.max_attempts,c.consumed_at,
			 c.superseded_at,c.locked_at,c.generation
			from accounts a join email_verification_challenges c on c.account_id=a.id
			where lower(a.email)=lower(?) order by c.generation desc limit 1 for update of a,c
			""", this::challengeRow, email);
	}

	int verifyUpdateEmailVerificationChallenges(int attempts, java.lang.Object value2, Timestamp nowTimestamp, UUID challengeId) {
		return db.update("update email_verification_challenges set attempt_count=?,locked_at=?,updated_at=?,version=version+1 where id=?", attempts, value2, nowTimestamp, challengeId);
	}

	int consumeChallenge(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID challengeId) {
		return db.update("update email_verification_challenges set consumed_at=?,updated_at=?,version=version+1 where id=?", nowTimestamp, nowTimestamp2, challengeId);
	}

	java.util.List<ResendRow> resendFindAccounts(java.lang.String email) {
		return db.query("""
			select a.id,a.registration_expires_at,c.id,c.generation,c.resend_available_at
			from accounts a join email_verification_challenges c on c.account_id=a.id
			where lower(a.email)=lower(?) and a.status='INACTIVE' and a.email_verification_required=true
			 and c.consumed_at is null and c.superseded_at is null for update of a,c
			""", (rs, n) -> new ResendRow(rs.getObject(1, UUID.class), instant(rs, 2), rs.getObject(3, UUID.class), rs.getInt(4), instant(rs, 5)), email);
	}

	int resendUpdateEmailVerificationChallenges(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID challengeId) {
		return db.update("update email_verification_challenges set superseded_at=?,updated_at=?,version=version+1 where id=?", nowTimestamp, nowTimestamp2, challengeId);
	}

	java.util.List<SessionRow> changeEmailFindEmailVerificationSessions(java.lang.String value2) {
		return db.query("""
			select s.id,s.account_id,s.csrf_token_hash,s.expires_at,s.change_email_count,a.registration_expires_at
			from email_verification_sessions s join accounts a on a.id=s.account_id
			where s.token_hash=? and s.revoked_at is null for update of s,a
			""", (rs, n) -> new SessionRow(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), instant(rs, 4), rs.getInt(5), instant(rs, 6)), value2);
	}

	int changeEmailUpdateAccounts(java.lang.String email, Timestamp nowTimestamp, UUID accountId, Timestamp nowTimestamp2) {
		return db.update("""
			update accounts set email=?,updated_at=?,version=version+1 where id=? and status='INACTIVE'
			 and email_verification_required=true and registration_expires_at>?
			""", email, nowTimestamp, accountId, nowTimestamp2);
	}

	java.lang.Integer changeEmailFindEmailVerificationChallenges(UUID accountId) {
		return db.queryForObject("select coalesce(max(generation),0) from email_verification_challenges where account_id=?", Integer.class, accountId);
	}

	int changeEmailUpdateEmailVerificationChallenges(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID accountId) {
		return db.update("update email_verification_challenges set superseded_at=?,updated_at=?,version=version+1 where account_id=? and consumed_at is null and superseded_at is null", nowTimestamp, nowTimestamp2, accountId);
	}

	int changeEmailUpdateEmailVerificationSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID sessionId) {
		return db.update("update email_verification_sessions set revoked_at=?,updated_at=? where id=?", nowTimestamp, nowTimestamp2, sessionId);
	}

	int changeEmailInsertEmailVerificationSessions(UUID randomUUID, UUID accountId, java.lang.String value3, java.lang.String value4, Timestamp value5, int value6) {
		return db.update("insert into email_verification_sessions(id,account_id,token_hash,csrf_token_hash,expires_at,change_email_count) values (?,?,?,?,?,?)", randomUUID, accountId, value3, value4, value5, value6);
	}

	int generateInsertEmailVerificationChallenges(UUID challengeId, UUID accountId, int generation, java.lang.String value4, java.lang.String hmacVersion, Timestamp expiresTimestamp, int maxAttempts, Timestamp resendTimestamp) {
		return db.update("""
			insert into email_verification_challenges(id,account_id,generation,otp_digest,hmac_key_version,expires_at,max_attempts,resend_available_at)
			values (?,?,?,?,?,?,?,?)
			""", challengeId, accountId, generation, value4, hmacVersion, expiresTimestamp, maxAttempts, resendTimestamp);
	}

	int revokeArtifactsUpdateEmailVerificationSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID accountId) {
		return db.update("update email_verification_sessions set revoked_at=?,updated_at=? where account_id=? and revoked_at is null", nowTimestamp, nowTimestamp2, accountId);
	}

	int revokeArtifactsUpdateEmailOutbox(Timestamp nowTimestamp, UUID accountId, int generation) {
		return db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and generation=? and status in ('PENDING','FAILED','PROCESSING')", nowTimestamp, accountId, generation);
	}

	int cancelOutboxUpdateEmailOutbox(Timestamp nowTimestamp, UUID accountId) {
		return db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and status in ('PENDING','FAILED','PROCESSING')", nowTimestamp, accountId);
	}

	java.util.List<ManualRequestAudit> cancelManualRequestsFindEmailVerificationManualActivationRequests(UUID accountId) {
		return db.query("select id,requested_by_account_id,reason_code,verification_method from email_verification_manual_activation_requests where account_id=? and status='PENDING' for update", (rs, n) -> new ManualRequestAudit(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4)), accountId);
	}

	int cancelManualRequestsUpdateEmailVerificationManualActivationRequests(java.lang.String terminalReason, Timestamp nowTimestamp, UUID accountId) {
		return db.update("update email_verification_manual_activation_requests set status='CANCELLED',terminal_reason=?,updated_at=?,version=version+1 where account_id=? and status='PENDING'", terminalReason, nowTimestamp, accountId);
	}

	java.lang.Integer cookieMatchesFindEmailVerificationSessions(java.lang.String value2, UUID accountId) {
		return db.queryForObject("select count(*) from email_verification_sessions where token_hash=? and account_id=?", Integer.class, value2, accountId);
	}

	private ChallengeRow challengeRow(ResultSet rs, int n) throws SQLException {
		return new ChallengeRow(rs.getObject(1, UUID.class), rs.getString(2), rs.getBoolean(3), instant(rs, 4), instant(rs, 5), rs.getObject(6, UUID.class), rs.getString(7), rs.getString(8), instant(rs, 9), rs.getInt(10), rs.getInt(11), instant(rs, 12), instant(rs, 13), instant(rs, 14), rs.getInt(15));
	}

	private Instant instant(ResultSet rs, int index) throws SQLException {
		Timestamp value = rs.getTimestamp(index);
		return value == null ? null : value.toInstant();
	}

	private final JdbcTemplate db;

	EmailVerificationRepository(JdbcTemplate db) {
		this.db = db;
	}
}
