package com.scms.backend.emailverification.manualactivation;

import com.scms.backend.account.AccountRole;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.ManualActivationReasonCode;
import com.scms.backend.emailverification.ManualActivationStatus;
import com.scms.backend.emailverification.ManualVerificationMethod;
import com.scms.backend.emailverification.manualactivation.ManualActivationService.PendingAccount;
import com.scms.backend.emailverification.manualactivation.ManualActivationService.RequestLock;
import com.scms.backend.emailverification.manualactivation.ManualActivationService.RequesterSnapshot;
import com.scms.backend.emailverification.verification.EmailVerificationService;
import com.scms.backend.emailverification.verification.PendingVerificationAccountResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

final class ManualActivationRepository {

	private final JdbcTemplate db;

	ManualActivationRepository(JdbcTemplate db) {
		this.db = db;
	}

	EmailVerificationException error(HttpStatus status, String code, String message) {
		return new EmailVerificationException(status, code, message);
	}

	String baseSelect() {
		return """
			select r.id,r.account_id,p.member_code,a.email,r.requested_by_account_id,r.requested_by_name_snapshot,
			 r.requested_by_role_snapshot,r.reason_code,r.reason_detail,
			 r.verification_method,r.evidence_note,r.status,r.reviewed_by_account_id,r.decision_note,r.requested_at,r.reviewed_at,r.expires_at
			from email_verification_manual_activation_requests r join accounts a on a.id=r.account_id join member_profiles p on p.account_id=a.id
			""";
	}

	ManualActivationResponse map(ResultSet rs, int n) throws SQLException {
		return new ManualActivationResponse(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), EmailVerificationService.mask(rs.getString(4)), rs.getObject(5, UUID.class), rs.getString(6), AccountRole.valueOf(rs.getString(7)), ManualActivationReasonCode.valueOf(rs.getString(8)), rs.getString(9), ManualVerificationMethod.valueOf(rs.getString(10)), rs.getString(11), ManualActivationStatus.valueOf(rs.getString(12)), rs.getObject(13, UUID.class), rs.getString(14), rs.getTimestamp(15).toInstant(), rs.getTimestamp(16) == null ? null : rs.getTimestamp(16).toInstant(), rs.getTimestamp(17).toInstant());
	}

	ManualActivationPageResponse activationPage(UUID actor, AccountRole role, ManualActivationStatus status, int page, int size) {
		String actorClause = role == AccountRole.RECEPTIONIST ? " and r.requested_by_account_id=?" : "";
		Object[] args = role == AccountRole.RECEPTIONIST ? new Object[] { status.name(), actor, size, page * size } : new Object[] { status.name(), size, page * size };
		List<ManualActivationResponse> content = db.query(baseSelect() + " where r.status=?" + actorClause + " order by r.requested_at desc,r.id desc limit ? offset ?", this::map, args);
		Long total = role == AccountRole.RECEPTIONIST ? db.queryForObject("select count(*) from email_verification_manual_activation_requests r where r.status=? and r.requested_by_account_id=?", Long.class, status.name(), actor) : db.queryForObject("select count(*) from email_verification_manual_activation_requests r where r.status=?", Long.class, status.name());
		long count = total == null ? 0 : total;
		return new ManualActivationPageResponse(content, page, size, count, (int) Math.ceil((double) count / size));
	}

	ManualActivationResponse activationDetail(UUID actor, UUID id, AccountRole role) {
		String suffix = role == AccountRole.RECEPTIONIST ? " and r.requested_by_account_id=?" : "";
		Object[] args = role == AccountRole.RECEPTIONIST ? new Object[] { id, actor } : new Object[] { id };
		List<ManualActivationResponse> rows = db.query(baseSelect() + " where r.id=?" + suffix, this::map, args);
		if (rows.isEmpty())
			throw error(HttpStatus.NOT_FOUND, "MANUAL_ACTIVATION_REQUEST_NOT_FOUND", "Manual activation request was not found.");
		return rows.getFirst();
	}

	List<PendingVerificationAccountResponse> searchFindAccounts(Timestamp nowTimestamp, java.lang.String q, java.lang.String q2, java.lang.String q3) {
		return db.query("""
			select a.id,p.member_code,a.full_name,a.phone,a.email,a.registration_expires_at
			from accounts a join member_profiles p on p.account_id=a.id
			where a.role='MEMBER' and a.status='INACTIVE' and a.email_verification_required=true and a.registration_expires_at>?
			and (upper(p.member_code)=upper(?) or a.phone=? or lower(a.email)=lower(?)) limit 2
			""", (rs, n) -> new PendingVerificationAccountResponse(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), EmailVerificationService.mask(rs.getString(5)), rs.getTimestamp(6).toInstant()), nowTimestamp, q, q2, q3);
	}

	List<PendingAccount> createFindAccounts(java.lang.String value2) {
		return db.query("""
			select a.id,p.member_code,a.email,a.registration_expires_at from accounts a join member_profiles p on p.account_id=a.id
			where upper(p.member_code)=upper(?) for update of a
			""", (rs, n) -> new PendingAccount(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant()), value2);
	}

	java.lang.Integer countEligibleInactiveMembers(UUID pendingId, Timestamp nowTimestamp) {
		return db.queryForObject("select count(*) from accounts where id=? and role='MEMBER' and status='INACTIVE' and email_verification_required=true and registration_expires_at>?", Integer.class, pendingId, nowTimestamp);
	}

	int createInsertEmailVerificationManualActivationRequests(UUID id, UUID pendingId, UUID receptionist, java.lang.String requester, java.lang.String value5, java.lang.String value6, java.lang.String value7, java.lang.String value8, java.lang.String value9, Timestamp nowTimestamp, Timestamp value11) {
		return db.update("""
			insert into email_verification_manual_activation_requests(id,account_id,requested_by_account_id,
			 requested_by_name_snapshot,requested_by_role_snapshot,reason_code,reason_detail,verification_method,
			 evidence_note,status,requested_at,expires_at)
			values (?,?,?,?,?,?,?,?,?,'PENDING',?,?)
			""", id, pendingId, receptionist, requester, value5, value6, value7, value8, value9, nowTimestamp, value11);
	}

	int approveUpdateEmailVerificationManualActivationRequests(UUID manager, java.lang.String value2, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID requestId) {
		return db.update("update email_verification_manual_activation_requests set status='APPROVED',reviewed_by_account_id=?,decision_note=?,reviewed_at=?,updated_at=?,version=version+1 where id=?", manager, value2, nowTimestamp, nowTimestamp2, requestId);
	}

	int rejectUpdateEmailVerificationManualActivationRequests(UUID manager, java.lang.String value2, Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID requestId) {
		return db.update("update email_verification_manual_activation_requests set status='REJECTED',reviewed_by_account_id=?,decision_note=?,reviewed_at=?,updated_at=?,version=version+1 where id=?", manager, value2, nowTimestamp, nowTimestamp2, requestId);
	}

	List<RequestLock> lockRequestFindEmailVerificationManualActivationRequests(UUID id) {
		return db.query("select account_id,requested_by_account_id,status,expires_at,reason_code,verification_method from email_verification_manual_activation_requests where id=? for update", (rs, n) -> new RequestLock(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), ManualActivationStatus.valueOf(rs.getString(3)), rs.getTimestamp(4).toInstant(), rs.getString(5), rs.getString(6)), id);
	}

	int terminateArtifactsUpdateEmailVerificationChallenges(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID account) {
		return db.update("update email_verification_challenges set superseded_at=coalesce(superseded_at,?),updated_at=?,version=version+1 where account_id=? and consumed_at is null", nowTimestamp, nowTimestamp2, account);
	}

	int terminateArtifactsUpdateEmailVerificationSessions(Timestamp nowTimestamp, Timestamp nowTimestamp2, UUID account) {
		return db.update("update email_verification_sessions set revoked_at=coalesce(revoked_at,?),updated_at=? where account_id=?", nowTimestamp, nowTimestamp2, account);
	}

	int terminateArtifactsUpdateEmailOutbox(Timestamp nowTimestamp, UUID account) {
		return db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and status in ('PENDING','FAILED','PROCESSING')", nowTimestamp, account);
	}

	RequesterSnapshot requesterSnapshotFindAccounts(UUID id) {
		return db.queryForObject("select full_name,role from accounts where id=?", (rs, n) -> new RequesterSnapshot(rs.getString(1), AccountRole.valueOf(rs.getString(2))), id);
	}
}
