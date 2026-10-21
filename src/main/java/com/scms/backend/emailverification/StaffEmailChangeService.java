package com.scms.backend.emailverification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.RefreshTokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffEmailChangeService {
	private final JdbcTemplate db; private final AccountRepository accounts; private final PasswordEncoder passwords;
	private final EmailVerificationProperties properties; private final EmailVerificationCrypto crypto;
	private final DatabaseRateLimiter limits; private final AuditEventRepository audits;
	private final RefreshTokenService refreshTokens; private final Clock clock;

	StaffEmailChangeService(JdbcTemplate db,AccountRepository accounts,PasswordEncoder passwords,
			EmailVerificationProperties properties,EmailVerificationCrypto crypto,DatabaseRateLimiter limits,
			AuditEventRepository audits,RefreshTokenService refreshTokens,Clock clock){
		this.db=db;this.accounts=accounts;this.passwords=passwords;this.properties=properties;this.crypto=crypto;
		this.limits=limits;this.audits=audits;this.refreshTokens=refreshTokens;this.clock=clock;}

	@Transactional
	public StaffEmailChangeResponse begin(UUID accountId,StaffEmailChangeRequest request){
		ensureEnabled();limits.check("STAFF_EMAIL_CHANGE_ACCOUNT",accountId.toString(),5,Duration.ofMinutes(15));
		Account account=staffForUpdate(accountId);if(!passwords.matches(request.currentPassword(),account.getPasswordHash()))
			throw error(HttpStatus.BAD_REQUEST,"CURRENT_PASSWORD_INCORRECT","The current password is incorrect.");
		String email=normalize(request.newEmail());if(email.equalsIgnoreCase(account.getEmail()))
			throw error(HttpStatus.CONFLICT,"EMAIL_UNCHANGED","The new email must differ from the current email.");
		if(accounts.existsByOccupiedEmail(email))throw emailExists();
		Integer pending=db.queryForObject("select count(*) from staff_email_change_requests where account_id=? and status='PENDING'",Integer.class,accountId);
		if(pending!=null&&pending>0)throw error(HttpStatus.CONFLICT,"EMAIL_CHANGE_PENDING","An email change is already pending.");
		Instant now=clock.instant(),expires=now.plus(properties.getOtpTtl()),resend=now.plus(properties.getResendCooldown());
		UUID id=UUID.randomUUID();String otp=crypto.otp(),hmacVersion=properties.getHmacActiveVersion();
		try{db.update("""
			insert into staff_email_change_requests(id,account_id,pending_email,otp_digest,hmac_key_version,expires_at,
			 resend_available_at,max_attempts,status,created_at,updated_at) values (?,?,?,?,?,?,?,?,'PENDING',?,?)
			""",id,accountId,email,crypto.otpDigest(otp,hmacVersion),hmacVersion,Timestamp.from(expires),Timestamp.from(resend),
			properties.getMaxAttempts(),Timestamp.from(now),Timestamp.from(now));}
		catch(DataIntegrityViolationException exception){throw emailExists();}
		enqueue(id,accountId,email,otp,1,expires,now);
		audits.save(new AuditEvent(UUID.randomUUID(),accountId,"STAFF_EMAIL_CHANGE_REQUESTED","ACCOUNT",accountId,
			Map.of("pendingEmail",EmailVerificationService.mask(email))));
		return response(email,expires,resend,now);
	}

	@Transactional(noRollbackFor=EmailVerificationException.class)
	public StaffEmailChangeVerifyResponse verify(UUID accountId,StaffEmailChangeVerifyRequest request){
		ensureEnabled();limits.check("STAFF_EMAIL_CHANGE_VERIFY",accountId.toString(),5,Duration.ofMinutes(1));
		Instant now=clock.instant();List<Row> rows=pendingForUpdate(accountId);
		if(rows.isEmpty()){
			List<String> verified=db.query("select pending_email from staff_email_change_requests where account_id=? and status='VERIFIED' order by verified_at desc limit 1",(rs,n)->rs.getString(1),accountId);
			if(!verified.isEmpty())return new StaffEmailChangeVerifyResponse(true,verified.getFirst());
			throw invalidOtp();
		}
		Row row=rows.getFirst();
		if(!row.expiresAt().isAfter(now)||row.attempts()>=row.maximum()){
			expire(row,now);throw invalidOtp();
		}
		if(!crypto.matchesOtp(request.otp(),row.digest(),row.keyVersion())){
			int attempts=row.attempts()+1;db.update("update staff_email_change_requests set attempt_count=?,status=?,updated_at=?,version=version+1 where id=?",
				attempts,attempts>=row.maximum()?"EXPIRED":"PENDING",Timestamp.from(now),row.id());throw invalidOtp();
		}
		Account account=staffForUpdate(accountId);
		try{account.changeVerifiedStaffEmail(row.email(),now);accounts.flush();}
		catch(DataIntegrityViolationException exception){throw emailExists();}
		db.update("update staff_email_change_requests set status='VERIFIED',verified_at=?,updated_at=?,version=version+1 where id=?",Timestamp.from(now),Timestamp.from(now),row.id());
		cancelOutbox(row.id(),now);refreshTokens.revokeAll(accountId);
		audits.save(new AuditEvent(UUID.randomUUID(),accountId,"STAFF_EMAIL_CHANGE_VERIFIED","ACCOUNT",accountId,
			Map.of("oldEmail",EmailVerificationService.mask(row.currentEmail()),"newEmail",EmailVerificationService.mask(row.email()))));
		return new StaffEmailChangeVerifyResponse(true,row.email());
	}

	@Transactional(noRollbackFor=EmailVerificationException.class)
	public StaffEmailChangeResponse resend(UUID accountId){
		ensureEnabled();limits.check("STAFF_EMAIL_CHANGE_RESEND",accountId.toString(),3,Duration.ofHours(1));
		Instant now=clock.instant();List<Row> rows=pendingForUpdate(accountId);if(rows.isEmpty())throw invalidOtp();Row row=rows.getFirst();
		if(!row.expiresAt().isAfter(now)) { expire(row,now);throw invalidOtp(); }
		if(row.resendAt().isAfter(now))throw new EmailVerificationException(HttpStatus.TOO_MANY_REQUESTS,"RESEND_COOLDOWN",
			"Wait before requesting another OTP.",Math.max(1,row.resendAt().getEpochSecond()-now.getEpochSecond()));
		String otp=crypto.otp();int generation=row.generation()+1;Instant expires=now.plus(properties.getOtpTtl()),resend=now.plus(properties.getResendCooldown());
		db.update("update staff_email_change_requests set generation=?,otp_digest=?,hmac_key_version=?,expires_at=?,resend_available_at=?,attempt_count=0,updated_at=?,version=version+1 where id=?",
			generation,crypto.otpDigest(otp,properties.getHmacActiveVersion()),properties.getHmacActiveVersion(),Timestamp.from(expires),Timestamp.from(resend),Timestamp.from(now),row.id());
		cancelOutbox(row.id(),now);enqueue(row.id(),accountId,row.email(),otp,generation,expires,now);
		audits.save(new AuditEvent(UUID.randomUUID(),accountId,"STAFF_EMAIL_CHANGE_OTP_RESENT","ACCOUNT",accountId,
			Map.of("pendingEmail",EmailVerificationService.mask(row.email()),"generation",generation)));
		return response(row.email(),expires,resend,now);
	}

	@Transactional
	public void cancel(UUID accountId){
		Instant now=clock.instant();List<Row> rows=pendingForUpdate(accountId);if(rows.isEmpty())return;Row row=rows.getFirst();
		db.update("update staff_email_change_requests set status='CANCELLED',cancelled_at=?,updated_at=?,version=version+1 where id=?",Timestamp.from(now),Timestamp.from(now),row.id());
		cancelOutbox(row.id(),now);audits.save(new AuditEvent(UUID.randomUUID(),accountId,"STAFF_EMAIL_CHANGE_CANCELLED","ACCOUNT",accountId,
			Map.of("pendingEmail",EmailVerificationService.mask(row.email()))));
	}

	@Transactional(readOnly=true)
	public StaffEmailChangeStatus status(UUID accountId){
		Instant now=clock.instant();List<StaffEmailChangeStatus> rows=db.query("select pending_email,expires_at,resend_available_at from staff_email_change_requests where account_id=? and status='PENDING' and expires_at>?",
			(rs,n)->new StaffEmailChangeStatus(true,EmailVerificationService.mask(rs.getString(1)),instant(rs,2),instant(rs,3)),accountId,Timestamp.from(now));
		return rows.isEmpty()?StaffEmailChangeStatus.none():rows.getFirst();
	}

	@Scheduled(cron="${scms.email-verification.cleanup-cron:0 0 * * * *}")
	@Transactional
	public void expirePending(){
		Instant now=clock.instant();List<Expired> rows=db.query("update staff_email_change_requests set status='EXPIRED',updated_at=?,version=version+1 where status='PENDING' and expires_at<=? returning id,account_id,pending_email",
			(rs,n)->new Expired(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3)),Timestamp.from(now),Timestamp.from(now));
		for(Expired row:rows){cancelOutbox(row.id(),now);audits.save(new AuditEvent(UUID.randomUUID(),"STAFF_EMAIL_CHANGE_EXPIRED","ACCOUNT",row.accountId(),Map.of("pendingEmail",EmailVerificationService.mask(row.email()))));}
	}

	private void enqueue(UUID requestId,UUID accountId,String email,String otp,int generation,Instant expires,Instant now){
		String version=properties.getEncryptionActiveVersion();var encrypted=crypto.encryptOtp(otp,version);
		db.update("""
			insert into email_outbox(id,event_key,account_id,generation,recipient_email,template_key,deliver_before,
			 payload_ciphertext,payload_nonce,encryption_key_version,status,next_retry_at)
			 values (?,?,?,?,?,'STAFF_EMAIL_CHANGE_OTP',?,?,?,?,'PENDING',?)
			""",UUID.randomUUID(),"STAFF_EMAIL_CHANGE_OTP:"+requestId+":"+generation,accountId,generation,email,Timestamp.from(expires),encrypted.ciphertext(),encrypted.nonce(),version,Timestamp.from(now));
	}
	private void cancelOutbox(UUID requestId,Instant now){db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where event_key like ? and status in ('PENDING','FAILED','PROCESSING')",Timestamp.from(now),"STAFF_EMAIL_CHANGE_OTP:"+requestId+":%");}
	private List<Row> pendingForUpdate(UUID accountId){return db.query("""
		select r.id,r.account_id,r.pending_email,r.generation,r.otp_digest,r.hmac_key_version,r.expires_at,r.resend_available_at,
		 r.attempt_count,r.max_attempts,a.email from staff_email_change_requests r join accounts a on a.id=r.account_id
		 where r.account_id=? and r.status='PENDING' for update of r
		""",this::row,accountId);}
	private Account staffForUpdate(UUID id){Account account=accounts.findByIdForUpdate(id).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"INVALID_TOKEN","The authenticated account no longer exists."));if(account.getRole()==AccountRole.MEMBER||account.getStatus()!=AccountStatus.ACTIVE)throw error(HttpStatus.FORBIDDEN,"STAFF_ACCOUNT_REQUIRED","An active staff account is required.");return account;}
	private void expire(Row row,Instant now){db.update("update staff_email_change_requests set status='EXPIRED',updated_at=?,version=version+1 where id=?",Timestamp.from(now),row.id());cancelOutbox(row.id(),now);audits.save(new AuditEvent(UUID.randomUUID(),"STAFF_EMAIL_CHANGE_EXPIRED","ACCOUNT",row.accountId(),Map.of("pendingEmail",EmailVerificationService.mask(row.email()))));}
	private StaffEmailChangeResponse response(String email,Instant expires,Instant resend,Instant now){return new StaffEmailChangeResponse(true,EmailVerificationService.mask(email),expires,resend,now);}
	private Row row(ResultSet rs,int n)throws SQLException{return new Row(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getInt(4),rs.getString(5),rs.getString(6),instant(rs,7),instant(rs,8),rs.getInt(9),rs.getInt(10),rs.getString(11));}
	private Instant instant(ResultSet rs,int index)throws SQLException{return rs.getTimestamp(index).toInstant();}
	private String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);}
	private void ensureEnabled(){if(!properties.isEnabled())throw error(HttpStatus.SERVICE_UNAVAILABLE,"EMAIL_VERIFICATION_UNAVAILABLE","Email verification is not configured.");}
	private EmailVerificationException invalidOtp(){return error(HttpStatus.BAD_REQUEST,"INVALID_OR_EXPIRED_OTP","The OTP is invalid or expired.");}
	private EmailVerificationException emailExists(){return error(HttpStatus.CONFLICT,"EMAIL_ALREADY_EXISTS","Email is already in use.");}
	private EmailVerificationException error(HttpStatus status,String code,String message){return new EmailVerificationException(status,code,message);}
	private record Row(UUID id,UUID accountId,String email,int generation,String digest,String keyVersion,Instant expiresAt,Instant resendAt,int attempts,int maximum,String currentEmail){}
	private record Expired(UUID id,UUID accountId,String email){}
}
