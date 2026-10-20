package com.scms.backend.emailverification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
	private final JdbcTemplate db; private final EmailVerificationProperties properties;
	private final EmailVerificationCrypto crypto; private final DatabaseRateLimiter limits; private final Clock clock;
	private final AuditEventRepository audits; private final NotificationWriter notifications; private final AccountRepository accounts;

	EmailVerificationService(JdbcTemplate db, EmailVerificationProperties properties, EmailVerificationCrypto crypto,
			DatabaseRateLimiter limits, Clock clock, AuditEventRepository audits, NotificationWriter notifications,
			AccountRepository accounts) {
		this.db=db; this.properties=properties; this.crypto=crypto; this.limits=limits; this.clock=clock;
		this.audits=audits; this.notifications=notifications; this.accounts=accounts;
	}

	public boolean enabled() { return properties.isEnabled(); }
	public Instant registrationExpiry() { return clock.instant().plus(properties.getRegistrationTtl()); }

	public void rateLimitRegister(String ip, String email, String phone) {
		if (!enabled()) return;
		limits.check("REGISTER_IP", ip, 10, Duration.ofMinutes(15));
		limits.check("REGISTER_EMAIL", normalizeEmail(email), 3, Duration.ofHours(1));
		limits.check("REGISTER_PHONE", phone.trim(), 3, Duration.ofHours(1));
	}

	@Transactional
	public EmailVerificationRegistration begin(UUID accountId, String email, Instant registrationExpiresAt) {
		Instant now=clock.instant(); Generated generated=generate(accountId, normalizeEmail(email), 1, now, registrationExpiresAt,
			"EMAIL_VERIFICATION_REQUESTED");
		String session=crypto.token(), csrf=crypto.token();
		db.update("""
			insert into email_verification_sessions(id,account_id,token_hash,csrf_token_hash,expires_at)
			values (?,?,?,?,?)
			""", UUID.randomUUID(), accountId, crypto.sha256(session), crypto.sha256(csrf), Timestamp.from(registrationExpiresAt));
		return new EmailVerificationRegistration(session, csrf, generated.expiresAt(), generated.resendAt(),
			registrationExpiresAt, now);
	}

	public PendingRegistrationException pendingConflict(String email) {
		Instant now=clock.instant();
		var rows=db.query("""
			select c.expires_at,c.resend_available_at,a.registration_expires_at from accounts a
			join email_verification_challenges c on c.account_id=a.id and c.consumed_at is null and c.superseded_at is null
			where lower(a.email)=lower(?) and a.email_verification_required=true and a.registration_expires_at>?
			""", (rs,n)->new PendingTimes(rs.getTimestamp(1).toInstant(),rs.getTimestamp(2).toInstant(),rs.getTimestamp(3).toInstant()),
			normalizeEmail(email), Timestamp.from(now));
		return rows.isEmpty()?null:new PendingRegistrationException(rows.getFirst().otp(),rows.getFirst().resend(),rows.getFirst().registration(),now);
	}

	@Transactional
	public VerifyResult verify(VerifyEmailRequest request, String cookie, String ip) {
		if (!enabled()) invalidOtp();
		limits.check("VERIFY_IP", ip, 5, Duration.ofMinutes(1));
		Instant now=clock.instant(); String email=normalizeEmail(request.email());
		var rows=db.query("""
			select a.id,a.status,a.email_verification_required,a.registration_expires_at,a.email_verified_at,
			 c.id,c.otp_digest,c.hmac_key_version,c.expires_at,c.attempt_count,c.max_attempts,c.consumed_at,
			 c.superseded_at,c.locked_at,c.generation
			from accounts a join email_verification_challenges c on c.account_id=a.id
			where lower(a.email)=lower(?) order by c.generation desc limit 1 for update of a,c
			""", this::challengeRow, email);
		if (rows.isEmpty()) invalidOtp();
		ChallengeRow row=rows.getFirst();
		boolean matches=crypto.matchesOtp(request.otp(),row.digest(),row.keyVersion());
		if (row.consumedAt()!=null) {
			if (matches && "ACTIVE".equals(row.status()) && !row.required() && row.verifiedAt()!=null)
				return new VerifyResult(new VerifyEmailResponse(true, com.scms.backend.account.AccountStatus.ACTIVE), cookieMatches(cookie,row.accountId()));
			invalidOtp();
		}
		if (!row.required() || !"INACTIVE".equals(row.status()) || row.registrationExpiresAt()==null
				|| !row.registrationExpiresAt().isAfter(now) || row.supersededAt()!=null || row.lockedAt()!=null
				|| !row.expiresAt().isAfter(now) || row.attempts()>=row.maximum()) invalidOtp();
		if (!matches) {
			int attempts=row.attempts()+1;
			db.update("update email_verification_challenges set attempt_count=?,locked_at=?,updated_at=?,version=version+1 where id=?",
				attempts, attempts>=row.maximum()?Timestamp.from(now):null, Timestamp.from(now), row.challengeId());
			invalidOtp();
		}
		var account=accounts.findById(row.accountId()).orElseThrow();
		try{account.verifyPendingEmail(now);}catch(IllegalStateException ex){invalidOtp();}
		accounts.flush();
		db.update("update email_verification_challenges set consumed_at=?,updated_at=?,version=version+1 where id=?",
			Timestamp.from(now),Timestamp.from(now),row.challengeId());
		revokeArtifacts(row.accountId(), now, row.generation());
		cancelManualRequests(row.accountId(),now,"ACCOUNT_ALREADY_ACTIVATED");
		audits.save(new AuditEvent(UUID.randomUUID(),"EMAIL_VERIFIED","ACCOUNT",row.accountId(),Map.of("source","SELF_REGISTRATION")));
		audits.save(new AuditEvent(UUID.randomUUID(),"ACCOUNT_ACTIVATED","ACCOUNT",row.accountId(),Map.of("source","EMAIL_OTP")));
		notifications.write("ACCOUNT_ACTIVATED:"+row.accountId(),row.accountId(),"ACCOUNT_ACTIVATED","ACCOUNT",row.accountId(),Map.of("source","EMAIL_OTP"));
		return new VerifyResult(new VerifyEmailResponse(true, com.scms.backend.account.AccountStatus.ACTIVE), cookieMatches(cookie,row.accountId()));
	}

	@Transactional
	public ResendEmailResponse resend(ResendEmailRequest request, String ip) {
		if (!enabled()) { Instant now=clock.instant(); return new ResendEmailResponse(true,now.plus(properties.getResendCooldown()),now); }
		limits.check("RESEND_IP", ip, 5, Duration.ofMinutes(1));
		String email=normalizeEmail(request.email()); limits.check("RESEND_EMAIL",email,3,Duration.ofHours(1));
		Instant now=clock.instant(), generic=now.plus(properties.getResendCooldown());
		var rows=db.query("""
			select a.id,a.registration_expires_at,c.id,c.generation,c.resend_available_at
			from accounts a join email_verification_challenges c on c.account_id=a.id
			where lower(a.email)=lower(?) and a.status='INACTIVE' and a.email_verification_required=true
			 and c.consumed_at is null and c.superseded_at is null for update of a,c
			""", (rs,n)->new ResendRow(rs.getObject(1,UUID.class),instant(rs,2),rs.getObject(3,UUID.class),rs.getInt(4),instant(rs,5)), email);
		if (!rows.isEmpty()) {
			ResendRow row=rows.getFirst();
			if (row.registrationExpiresAt().isAfter(now) && !row.resendAt().isAfter(now)) {
				db.update("update email_verification_challenges set superseded_at=?,updated_at=?,version=version+1 where id=?",Timestamp.from(now),Timestamp.from(now),row.challengeId());
				cancelOutbox(row.accountId(),now);
				generate(row.accountId(),email,row.generation()+1,now,row.registrationExpiresAt(),"EMAIL_VERIFICATION_RESENT");
			}
		}
		return new ResendEmailResponse(true,generic,now);
	}

	@Transactional
	public ChangeResult changeEmail(ChangePendingEmailRequest request, String cookie, String csrf, String ip) {
		if (!enabled()) invalidSession();
		limits.check("CHANGE_EMAIL_IP",ip,5,Duration.ofMinutes(1)); Instant now=clock.instant();
		if (cookie==null || cookie.isBlank() || csrf==null || csrf.isBlank()) invalidSession();
		var sessions=db.query("""
			select s.id,s.account_id,s.csrf_token_hash,s.expires_at,s.change_email_count,a.registration_expires_at
			from email_verification_sessions s join accounts a on a.id=s.account_id
			where s.token_hash=? and s.revoked_at is null for update of s,a
			""", (rs,n)->new SessionRow(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),instant(rs,4),rs.getInt(5),instant(rs,6)),crypto.sha256(cookie));
		if (sessions.isEmpty()) invalidSession(); SessionRow session=sessions.getFirst();
		if (!session.expiresAt().isAfter(now) || !crypto.secureHashMatch(csrf,session.csrfHash())) invalidSession();
		if (session.changeCount()>=3) throw new EmailVerificationException(HttpStatus.CONFLICT,"EMAIL_CHANGE_LIMIT_EXCEEDED","Email change limit exceeded.");
		String email=normalizeEmail(request.newEmail());
		try { int changed=db.update("""
			update accounts set email=?,updated_at=?,version=version+1 where id=? and status='INACTIVE'
			 and email_verification_required=true and registration_expires_at>?
			""",email,Timestamp.from(now),session.accountId(),Timestamp.from(now)); if(changed!=1) invalidSession(); }
		catch(DataIntegrityViolationException ex){throw new EmailVerificationException(HttpStatus.CONFLICT,"EMAIL_ALREADY_EXISTS","Email is already in use.");}
		Integer generation=db.queryForObject("select coalesce(max(generation),0) from email_verification_challenges where account_id=?",Integer.class,session.accountId());
		db.update("update email_verification_challenges set superseded_at=?,updated_at=?,version=version+1 where account_id=? and consumed_at is null and superseded_at is null",Timestamp.from(now),Timestamp.from(now),session.accountId());
		cancelOutbox(session.accountId(),now);
		db.update("update email_verification_sessions set revoked_at=?,updated_at=? where id=?",Timestamp.from(now),Timestamp.from(now),session.id());
		cancelManualRequests(session.accountId(),now,"EMAIL_CHANGED");
		Generated generated=generate(session.accountId(),email,(generation==null?0:generation)+1,now,session.registrationExpiresAt(),"EMAIL_VERIFICATION_EMAIL_CHANGED");
		String newToken=crypto.token(), newCsrf=crypto.token();
		db.update("insert into email_verification_sessions(id,account_id,token_hash,csrf_token_hash,expires_at,change_email_count) values (?,?,?,?,?,?)",
			UUID.randomUUID(),session.accountId(),crypto.sha256(newToken),crypto.sha256(newCsrf),Timestamp.from(session.registrationExpiresAt()),session.changeCount()+1);
		return new ChangeResult(new ChangePendingEmailResponse(true,mask(email),newCsrf,generated.expiresAt(),generated.resendAt(),session.registrationExpiresAt(),now),newToken);
	}

	private Generated generate(UUID accountId,String email,int generation,Instant now,Instant registrationExpiresAt,String auditAction) {
		String otp=crypto.otp(); Instant expires=min(now.plus(properties.getOtpTtl()),registrationExpiresAt);
		Instant resend=now.plus(properties.getResendCooldown()); String hmacVersion=properties.getHmacActiveVersion();
		UUID challengeId=UUID.randomUUID();
		db.update("""
			insert into email_verification_challenges(id,account_id,generation,otp_digest,hmac_key_version,expires_at,max_attempts,resend_available_at)
			values (?,?,?,?,?,?,?,?)
			""",challengeId,accountId,generation,crypto.otpDigest(otp,hmacVersion),hmacVersion,Timestamp.from(expires),properties.getMaxAttempts(),Timestamp.from(resend));
		String encryptionVersion=properties.getEncryptionActiveVersion(); var encrypted=crypto.encryptOtp(otp,encryptionVersion);
		db.update("""
			insert into email_outbox(id,event_key,account_id,generation,recipient_email,template_key,deliver_before,
			 payload_ciphertext,payload_nonce,encryption_key_version,status,next_retry_at)
			values (?,?,?,?,?,'EMAIL_VERIFICATION_OTP',?,?,?,?, 'PENDING',?)
			""",UUID.randomUUID(),"EMAIL_VERIFICATION_OTP:"+accountId+":"+generation,accountId,generation,email,Timestamp.from(expires),
			encrypted.ciphertext(),encrypted.nonce(),encryptionVersion,Timestamp.from(now));
		audits.save(new AuditEvent(UUID.randomUUID(),auditAction,"ACCOUNT",accountId,Map.of("source","SELF_REGISTRATION","generation",generation)));
		return new Generated(expires,resend);
	}

	private void revokeArtifacts(UUID accountId,Instant now,int generation){
		db.update("update email_verification_sessions set revoked_at=?,updated_at=? where account_id=? and revoked_at is null",Timestamp.from(now),Timestamp.from(now),accountId);
		db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and generation=? and status in ('PENDING','FAILED','PROCESSING')",Timestamp.from(now),accountId,generation);
	}
	private void cancelOutbox(UUID accountId,Instant now){db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and status in ('PENDING','FAILED','PROCESSING')",Timestamp.from(now),accountId);}
	private void cancelManualRequests(UUID accountId,Instant now,String terminalReason){
		var requests=db.query("select id,requested_by_account_id,reason_code,verification_method from email_verification_manual_activation_requests where account_id=? and status='PENDING' for update",
			(rs,n)->new ManualRequestAudit(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4)),accountId);
		db.update("update email_verification_manual_activation_requests set status='CANCELLED',terminal_reason=?,updated_at=?,version=version+1 where account_id=? and status='PENDING'",terminalReason,Timestamp.from(now),accountId);
		for(var request:requests)audits.save(new AuditEvent(UUID.randomUUID(),"MANUAL_ACTIVATION_REQUEST_CANCELLED","MANUAL_ACTIVATION_REQUEST",request.id(),
			Map.of("accountId",accountId.toString(),"requestedByAccountId",request.requestedBy().toString(),"reasonCode",request.reasonCode(),
				"verificationMethod",request.verificationMethod(),"terminalReason",terminalReason,"source","SELF_REGISTRATION")));
	}
	private boolean cookieMatches(String cookie,UUID accountId){if(cookie==null||cookie.isBlank())return false; Integer count=db.queryForObject("select count(*) from email_verification_sessions where token_hash=? and account_id=?",Integer.class,crypto.sha256(cookie),accountId);return count!=null&&count>0;}
	private void invalidOtp(){throw new EmailVerificationException(HttpStatus.BAD_REQUEST,"INVALID_OR_EXPIRED_OTP","The OTP is invalid or expired.");}
	private void invalidSession(){throw new EmailVerificationException(HttpStatus.UNAUTHORIZED,"INVALID_PENDING_SESSION","The pending verification session is invalid or expired.");}
	private String normalizeEmail(String email){return email.trim().toLowerCase(java.util.Locale.ROOT);}
	public static String mask(String email){int at=email.indexOf('@'); return at<1?"***":email.substring(0,1)+"***"+email.substring(at);}
	private Instant min(Instant a,Instant b){return a.isBefore(b)?a:b;}
	private Instant instant(ResultSet rs,int index)throws SQLException{Timestamp value=rs.getTimestamp(index);return value==null?null:value.toInstant();}
	private ChallengeRow challengeRow(ResultSet rs,int n)throws SQLException{return new ChallengeRow(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3),instant(rs,4),instant(rs,5),rs.getObject(6,UUID.class),rs.getString(7),rs.getString(8),instant(rs,9),rs.getInt(10),rs.getInt(11),instant(rs,12),instant(rs,13),instant(rs,14),rs.getInt(15));}

	public record VerifyResult(VerifyEmailResponse response,boolean clearCookie){}
	public record ChangeResult(ChangePendingEmailResponse response,String sessionToken){}
	private record Generated(Instant expiresAt,Instant resendAt){}
	private record PendingTimes(Instant otp,Instant resend,Instant registration){}
	private record ResendRow(UUID accountId,Instant registrationExpiresAt,UUID challengeId,int generation,Instant resendAt){}
	private record SessionRow(UUID id,UUID accountId,String csrfHash,Instant expiresAt,int changeCount,Instant registrationExpiresAt){}
	private record ManualRequestAudit(UUID id,UUID requestedBy,String reasonCode,String verificationMethod){}
	private record ChallengeRow(UUID accountId,String status,boolean required,Instant registrationExpiresAt,Instant verifiedAt,UUID challengeId,String digest,String keyVersion,Instant expiresAt,int attempts,int maximum,Instant consumedAt,Instant supersededAt,Instant lockedAt,int generation){}
}
