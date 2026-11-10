package com.scms.backend.emailverification.emailchange;

import com.scms.backend.emailverification.AccountEmailChangeStatus;
import com.scms.backend.emailverification.emailchange.AccountEmailChangeService.Expired;
import com.scms.backend.emailverification.emailchange.AccountEmailChangeService.Row;
import com.scms.backend.emailverification.verification.EmailVerificationService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class AccountEmailChangeRepository {

	int createInsertStaffEmailChangeRequests(UUID id, UUID accountId, java.lang.String email, int generation, java.lang.String value5, java.lang.String hmacVersion, Timestamp expiresTimestamp, Timestamp resendTimestamp, int maxAttempts, Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.update("""
			insert into staff_email_change_requests(id,account_id,pending_email,generation,otp_digest,
			 hmac_key_version,expires_at,resend_available_at,max_attempts,status,created_at,updated_at,purpose_bound)
			values (?,?,?,?,?,?,?,?,?,'PENDING',?,?,true)
			""", id, accountId, email, generation, value5, hmacVersion, expiresTimestamp, resendTimestamp, maxAttempts, nowTimestamp, nowTimestamp2);
	}

	List<java.lang.String> verifyFindStaffEmailChangeRequests(UUID accountId) {
		return db.query("""
			select pending_email from staff_email_change_requests where account_id=?
			and status in ('COMPLETED','VERIFIED') order by coalesce(completed_at,verified_at) desc nulls last limit 1
			""", (rs, n) -> rs.getString(1), accountId);
	}

	int verifyUpdateStaffEmailChangeRequests(int attempts, java.lang.String status, Timestamp nowTimestamp, UUID rowId) {
		return db.update("update staff_email_change_requests set attempt_count=?,status=?,updated_at=?,version=version+1 where id=?", attempts, status, nowTimestamp, rowId);
	}

	int markEmailChangeCompleted(Timestamp nowTimestamp, Timestamp nowTimestamp2, Timestamp nowTimestamp3, UUID rowId) {
		return db.update("""
			update staff_email_change_requests set status='COMPLETED',verified_at=?,completed_at=?,updated_at=?,
			 version=version+1 where id=?
			""", nowTimestamp, nowTimestamp2, nowTimestamp3, rowId);
	}

	int resendUpdateStaffEmailChangeRequests(int generation, java.lang.String value2, java.lang.String hmacVersion, Timestamp expiresTimestamp, Timestamp resendTimestamp, Timestamp nowTimestamp, UUID rowId) {
		return db.update("""
			update staff_email_change_requests set generation=?,otp_digest=?,hmac_key_version=?,expires_at=?,
			 resend_available_at=?,attempt_count=0,purpose_bound=true,updated_at=?,version=version+1 where id=?
			""", generation, value2, hmacVersion, expiresTimestamp, resendTimestamp, nowTimestamp, rowId);
	}

	int cancelUpdateStaffEmailChangeRequests(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID rowId) {
		return db.update("update staff_email_change_requests set status='CANCELLED',cancelled_at=?,updated_at=?,version=version+1 where id=?", nowTimestamp, nowTimestamp2, rowId);
	}

	List<AccountEmailChangeStatus> statusFindStaffEmailChangeRequests(UUID accountId, Timestamp nowTimestamp) {
		return db.query("""
			select pending_email,expires_at,resend_available_at from staff_email_change_requests
			where account_id=? and status='PENDING' and expires_at>?
			""", (rs, n) -> new AccountEmailChangeStatus(true, EmailVerificationService.mask(rs.getString(1)), instant(rs, 2), instant(rs, 3)), accountId, nowTimestamp);
	}

	List<Expired> expirePendingFindStaffEmailChangeRequests(Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.query("""
			update staff_email_change_requests set status='EXPIRED',updated_at=?,version=version+1
			where status='PENDING' and expires_at<=? returning id,account_id,pending_email
			""", (rs, n) -> new Expired(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3)), nowTimestamp, nowTimestamp2);
	}

	int supersedeUpdateStaffEmailChangeRequests(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID rowId) {
		return db.update("update staff_email_change_requests set status='SUPERSEDED',superseded_at=?,updated_at=?,version=version+1 where id=?", nowTimestamp, nowTimestamp2, rowId);
	}

	int expireUpdateStaffEmailChangeRequests(Timestamp nowTimestamp, UUID rowId) {
		return db.update("update staff_email_change_requests set status='EXPIRED',updated_at=?,version=version+1 where id=?", nowTimestamp, rowId);
	}

	int cancelOutboxUpdateEmailOutbox(Timestamp nowTimestamp, java.lang.String value2, java.lang.String value3) {
		return db.update("""
			update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,
			 locked_by=null,updated_at=? where (event_key like ? or event_key like ?)
			 and status in ('PENDING','FAILED','PROCESSING')
			""", nowTimestamp, value2, value3);
	}

	List<Row> pendingForUpdateFindStaffEmailChangeRequests(UUID accountId) {
		return db.query("""
			select r.id,r.account_id,r.pending_email,r.generation,r.otp_digest,r.hmac_key_version,r.expires_at,
			 r.resend_available_at,r.attempt_count,r.max_attempts,a.email,r.purpose_bound
			from staff_email_change_requests r join accounts a on a.id=r.account_id
			where r.account_id=? and r.status='PENDING' for update of r
			""", this::row, accountId);
	}

	List<java.lang.Integer> lockEmailClaimFindRows(java.lang.String email) {
		return db.query("select pg_advisory_xact_lock(hashtextextended(lower(?),0))", (rs, n) -> 0, email);
	}

	private Instant instant(ResultSet rs, int index) throws SQLException {
		return rs.getTimestamp(index).toInstant();
	}

	private Row row(ResultSet rs, int n) throws SQLException {
		return new Row(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getString(6), instant(rs, 7), instant(rs, 8), rs.getInt(9), rs.getInt(10), rs.getString(11), rs.getBoolean(12));
	}

	private final JdbcTemplate db;

	AccountEmailChangeRepository(JdbcTemplate db) {
		this.db = db;
	}
}
