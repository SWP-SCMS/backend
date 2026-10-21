package com.scms.backend.emailverification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class EmailOutboxDeliveryService {
	private final JdbcTemplate db; private final EmailVerificationCrypto crypto; private final EmailProvider provider;
	private final AuditEventRepository audits;
	EmailOutboxDeliveryService(JdbcTemplate db,EmailVerificationCrypto crypto,EmailProvider provider,AuditEventRepository audits){this.db=db;this.crypto=crypto;this.provider=provider;this.audits=audits;}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	Optional<EmailJob> claim(Instant now,String worker){
		List<EmailJob> rows=db.query("""
			with candidate as (
			 select id from email_outbox where
			 ((status in ('PENDING','FAILED') and next_retry_at<=?) or (status='PROCESSING' and locked_at<?))
			 and deliver_before>? order by created_at for update skip locked limit 1)
			update email_outbox e set status='PROCESSING',locked_at=?,locked_by=?,retry_count=retry_count+1,updated_at=?
			from candidate c where e.id=c.id returning e.id,e.account_id,e.recipient_email,e.deliver_before,
			 e.payload_ciphertext,e.payload_nonce,e.encryption_key_version,e.retry_count
			""",this::map,Timestamp.from(now),Timestamp.from(now.minus(Duration.ofMinutes(5))),Timestamp.from(now),
			Timestamp.from(now),worker,Timestamp.from(now));
		return rows.stream().findFirst().map(job -> new EmailJob(job.id(),job.accountId(),job.recipient(),
			job.deliverBefore(),job.ciphertext(),job.nonce(),job.keyVersion(),job.retryCount(),
			db.queryForObject("select full_name from accounts where id=?",String.class,job.accountId())));
	}

	void deliver(EmailJob job,Instant now){String otp=crypto.decryptOtp(job.ciphertext(),job.nonce(),job.keyVersion());
		provider.sendVerificationOtp(job.recipient(),job.recipientName(),otp,
			Math.max(1,Duration.between(now,job.deliverBefore()).toMinutes()));}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void sent(UUID id,Instant now){db.update("update email_outbox set status='SENT',sent_at=?,payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,last_error=null,updated_at=? where id=?",Timestamp.from(now),Timestamp.from(now),id);}
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void failed(EmailJob job,Instant now){long[] delays={30,120,300};long delay=delays[Math.min(job.retryCount()-1,delays.length-1)];Instant next=now.plusSeconds(delay);
		if(job.retryCount()>=3||!next.plusSeconds(10).isBefore(job.deliverBefore())) terminal(job.id(),now);
		else db.update("update email_outbox set status='FAILED',next_retry_at=?,locked_at=null,locked_by=null,last_error='SMTP_DELIVERY_FAILED',updated_at=? where id=?",Timestamp.from(next),Timestamp.from(now),job.id());}
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	void terminal(UUID id,Instant now){List<UUID> accounts=db.query("update email_outbox set status='TERMINAL_FAILED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,last_error='DELIVERY_WINDOW_EXHAUSTED',updated_at=? where id=? and status<>'TERMINAL_FAILED' returning account_id",(rs,n)->rs.getObject(1,UUID.class),Timestamp.from(now),id);accounts.forEach(account->auditTerminal(account,id,"DELIVERY_WINDOW_EXHAUSTED"));}
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	int expire(Instant now){var rows=db.query("update email_outbox set status='TERMINAL_FAILED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,last_error='OTP_EXPIRED',updated_at=? where status in ('PENDING','FAILED','PROCESSING') and deliver_before<=? returning id,account_id",(rs,n)->new TerminalJob(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),Timestamp.from(now),Timestamp.from(now));rows.forEach(job->auditTerminal(job.accountId(),job.id(),"OTP_EXPIRED"));return rows.size();}
	private void auditTerminal(UUID accountId,UUID outboxId,String reason){audits.save(new AuditEvent(UUID.randomUUID(),"EMAIL_DELIVERY_TERMINAL_FAILED","ACCOUNT",accountId,java.util.Map.of("outboxId",outboxId.toString(),"reason",reason)));}

	private EmailJob map(ResultSet rs,int n)throws SQLException{return new EmailJob(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getTimestamp(4).toInstant(),rs.getBytes(5),rs.getBytes(6),rs.getString(7),rs.getInt(8),null);}
	record EmailJob(UUID id,UUID accountId,String recipient,Instant deliverBefore,byte[] ciphertext,byte[] nonce,String keyVersion,int retryCount,String recipientName){}
	private record TerminalJob(UUID id,UUID accountId){}
}
