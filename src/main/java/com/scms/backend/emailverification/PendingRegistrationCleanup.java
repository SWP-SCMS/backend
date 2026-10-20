package com.scms.backend.emailverification;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PendingRegistrationCleanup {
	private final JdbcTemplate db;private final AccountRepository accounts;private final AuditEventRepository audits;private final Clock clock;
	PendingRegistrationCleanup(JdbcTemplate db,AccountRepository accounts,AuditEventRepository audits,Clock clock){this.db=db;this.accounts=accounts;this.audits=audits;this.clock=clock;}
	@Scheduled(cron="${scms.email-verification.cleanup-cron:0 0 * * * *}")
	@Transactional
	void cleanup(){Instant now=clock.instant();List<UUID> ids=db.query("select id from accounts where status='INACTIVE' and email_verification_required=true and registration_expires_at<=? order by registration_expires_at for update skip locked limit 200",(rs,n)->rs.getObject(1,UUID.class),Timestamp.from(now));
		for(UUID id:ids){Account account=accounts.findById(id).orElseThrow();account.expirePendingRegistration(now);
			var manualRequests=db.query("select id,requested_by_account_id,reason_code,verification_method from email_verification_manual_activation_requests where account_id=? and status='PENDING' for update",
				(rs,n)->new ManualRequestAudit(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4)),id);
			db.update("update email_verification_challenges set superseded_at=coalesce(superseded_at,?),updated_at=?,version=version+1 where account_id=? and consumed_at is null",Timestamp.from(now),Timestamp.from(now),id);
			db.update("update email_verification_sessions set revoked_at=coalesce(revoked_at,?),updated_at=? where account_id=?",Timestamp.from(now),Timestamp.from(now),id);
			db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and status in ('PENDING','FAILED','PROCESSING')",Timestamp.from(now),id);
			db.update("update refresh_tokens set revoked_at=coalesce(revoked_at,?),version=version+1 where account_id=? and revoked_at is null",Timestamp.from(now),id);
			db.update("update email_verification_manual_activation_requests set status='EXPIRED',terminal_reason='REGISTRATION_EXPIRED',updated_at=?,version=version+1 where account_id=? and status='PENDING'",Timestamp.from(now),id);
			for(var request:manualRequests)audits.save(new AuditEvent(UUID.randomUUID(),"MANUAL_ACTIVATION_REQUEST_EXPIRED","MANUAL_ACTIVATION_REQUEST",request.id(),
				Map.of("accountId",id.toString(),"requestedByAccountId",request.requestedBy().toString(),"reasonCode",request.reasonCode(),
					"verificationMethod",request.verificationMethod(),"terminalReason","REGISTRATION_EXPIRED","source","SCHEDULED_CLEANUP")));
			audits.save(new AuditEvent(UUID.randomUUID(),"EMAIL_VERIFICATION_REGISTRATION_EXPIRED","ACCOUNT",id,Map.of("source","SCHEDULED_CLEANUP")));}
		accounts.flush();db.update("delete from email_verification_rate_limits where expires_at<?",Timestamp.from(now.minusSeconds(3600)));
	}
	private record ManualRequestAudit(UUID id,UUID requestedBy,String reasonCode,String verificationMethod){}
}
