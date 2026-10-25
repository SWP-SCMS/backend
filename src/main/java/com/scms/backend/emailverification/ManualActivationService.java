package com.scms.backend.emailverification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ManualActivationService {
	private final JdbcTemplate db;private final AccountRepository accounts;private final AuditEventRepository audits;
	private final NotificationWriter notifications;private final DatabaseRateLimiter limits;private final Clock clock;
	ManualActivationService(JdbcTemplate db,AccountRepository accounts,AuditEventRepository audits,
		NotificationWriter notifications,DatabaseRateLimiter limits,Clock clock){this.db=db;this.accounts=accounts;this.audits=audits;
		this.notifications=notifications;this.limits=limits;this.clock=clock;}

	@Transactional(readOnly=true)
	PendingVerificationAccountResponse search(UUID receptionist,String query){ensure(receptionist,AccountRole.RECEPTIONIST);String q=query==null?"":query.trim();
		if(q.isEmpty())throw new EmailVerificationException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","query must not be blank.");
		Instant now=clock.instant();List<PendingVerificationAccountResponse> rows=db.query("""
			select a.id,p.member_code,a.full_name,a.phone,a.email,a.registration_expires_at
			from accounts a join member_profiles p on p.account_id=a.id
			where a.role='MEMBER' and a.status='INACTIVE' and a.email_verification_required=true and a.registration_expires_at>?
			and (upper(p.member_code)=upper(?) or a.phone=? or lower(a.email)=lower(?)) limit 2
			""",(rs,n)->new PendingVerificationAccountResponse(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),
			EmailVerificationService.mask(rs.getString(5)),rs.getTimestamp(6).toInstant()),Timestamp.from(now),q,q,q);
		if(rows.size()!=1)throw error(HttpStatus.NOT_FOUND,"PENDING_ACCOUNT_NOT_FOUND","Pending account was not found.");return rows.getFirst();}

	@Transactional
	ManualActivationResponse create(UUID receptionist,ManualActivationCreateRequest request){ensure(receptionist,AccountRole.RECEPTIONIST);
		limits.check("MANUAL_CREATE_ACTOR",receptionist.toString(),10,Duration.ofHours(1));Instant now=clock.instant();
		RequesterSnapshot requester=requesterSnapshot(receptionist);
		var rows=db.query("""
			select a.id,p.member_code,a.email,a.registration_expires_at from accounts a join member_profiles p on p.account_id=a.id
			where upper(p.member_code)=upper(?) for update of a
			""",(rs,n)->new PendingAccount(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getTimestamp(4)==null?null:rs.getTimestamp(4).toInstant()),request.memberId().trim());
		if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"PENDING_ACCOUNT_NOT_FOUND","Pending account was not found.");PendingAccount pending=rows.getFirst();
		Integer eligible=db.queryForObject("select count(*) from accounts where id=? and role='MEMBER' and status='INACTIVE' and email_verification_required=true and registration_expires_at>?",Integer.class,pending.id(),Timestamp.from(now));
		if(eligible==null||eligible!=1)throw error(HttpStatus.CONFLICT,"MANUAL_ACTIVATION_NOT_ELIGIBLE","Account is not eligible for manual activation.");
		UUID id=UUID.randomUUID();try{db.update("""
			insert into email_verification_manual_activation_requests(id,account_id,requested_by_account_id,
			 requested_by_name_snapshot,requested_by_role_snapshot,reason_code,reason_detail,verification_method,
			 evidence_note,status,requested_at,expires_at)
			values (?,?,?,?,?,?,?,?,?,'PENDING',?,?)
			""",id,pending.id(),receptionist,requester.name(),requester.role().name(),request.reasonCode().name(),
			request.reasonDetail().trim(),request.verificationMethod().name(),
			request.evidenceNote().trim(),Timestamp.from(now),Timestamp.from(pending.expiresAt()));}
		catch(DataIntegrityViolationException ex){throw error(HttpStatus.CONFLICT,"MANUAL_ACTIVATION_REQUEST_PENDING","A manual activation request is already pending.");}
		audits.save(new AuditEvent(UUID.randomUUID(),receptionist,"MANUAL_ACTIVATION_REQUESTED","MANUAL_ACTIVATION_REQUEST",id,
			Map.of("accountId",pending.id().toString(),"requestId",id.toString(),"reasonCode",request.reasonCode().name(),"verificationMethod",request.verificationMethod().name())));
		return getForActor(receptionist,id,AccountRole.RECEPTIONIST);
	}

	@Transactional(readOnly=true)
	ManualActivationPageResponse list(UUID actor,AccountRole role,ManualActivationStatus status,int page,int size){ensure(actor,role);validatePage(page,size);
		String actorClause=role==AccountRole.RECEPTIONIST?" and r.requested_by_account_id=?":"";
		Object[] args=role==AccountRole.RECEPTIONIST?new Object[]{status.name(),actor,size,page*size}:new Object[]{status.name(),size,page*size};
		List<ManualActivationResponse> content=db.query(baseSelect()+" where r.status=?"+actorClause+" order by r.requested_at desc,r.id desc limit ? offset ?",this::map,args);
		Long total=role==AccountRole.RECEPTIONIST?db.queryForObject("select count(*) from email_verification_manual_activation_requests r where r.status=? and r.requested_by_account_id=?",Long.class,status.name(),actor)
			:db.queryForObject("select count(*) from email_verification_manual_activation_requests r where r.status=?",Long.class,status.name());
		long count=total==null?0:total;return new ManualActivationPageResponse(content,page,size,count,(int)Math.ceil((double)count/size));}

	@Transactional(readOnly=true)
	ManualActivationResponse getForActor(UUID actor,UUID id,AccountRole role){ensure(actor,role);String suffix=role==AccountRole.RECEPTIONIST?" and r.requested_by_account_id=?":"";
		Object[] args=role==AccountRole.RECEPTIONIST?new Object[]{id,actor}:new Object[]{id};List<ManualActivationResponse> rows=db.query(baseSelect()+" where r.id=?"+suffix,this::map,args);
		if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"MANUAL_ACTIVATION_REQUEST_NOT_FOUND","Manual activation request was not found.");return rows.getFirst();}

	@Transactional
	ManualActivationResponse approve(UUID manager,UUID requestId,ManualActivationDecisionRequest request){ensure(manager,AccountRole.MANAGER);limits.check("MANUAL_DECISION_ACTOR",manager.toString(),30,Duration.ofMinutes(1));
		Instant now=clock.instant();RequestLock locked=lockRequest(requestId);if(locked.status()!=ManualActivationStatus.PENDING)
			throw error(HttpStatus.CONFLICT,"MANUAL_ACTIVATION_REQUEST_DECIDED","Manual activation request is no longer pending.");
		Account account=accounts.findByIdForUpdate(locked.accountId()).orElseThrow();
		if(!locked.expiresAt().isAfter(now))throw error(HttpStatus.CONFLICT,"MANUAL_ACTIVATION_NOT_ELIGIBLE","Manual activation request has expired.");
		try{account.approveManualEmailVerificationBypass(now);}catch(IllegalStateException ex){throw error(HttpStatus.CONFLICT,"MANUAL_ACTIVATION_NOT_ELIGIBLE","Account is not eligible for manual activation.");}
		accounts.flush();db.update("update email_verification_manual_activation_requests set status='APPROVED',reviewed_by_account_id=?,decision_note=?,reviewed_at=?,updated_at=?,version=version+1 where id=?",manager,request.decisionNote().trim(),Timestamp.from(now),Timestamp.from(now),requestId);
		terminateArtifacts(locked.accountId(),now);
		Map<String,Object> data=Map.of("requestId",requestId.toString(),"accountId",locked.accountId().toString(),
			"requestedByAccountId",locked.requestedBy().toString(),"approvedByAccountId",manager.toString(),
			"reasonCode",locked.reasonCode(),"verificationMethod",locked.verificationMethod());
		audits.save(new AuditEvent(UUID.randomUUID(),manager,"MANUAL_ACTIVATION_REQUEST_APPROVED","MANUAL_ACTIVATION_REQUEST",requestId,data));
		audits.save(new AuditEvent(UUID.randomUUID(),manager,"EMAIL_VERIFICATION_BYPASSED","ACCOUNT",locked.accountId(),data));
		audits.save(new AuditEvent(UUID.randomUUID(),manager,"ACCOUNT_ACTIVATED_MANUALLY","ACCOUNT",locked.accountId(),data));
		notifications.write("ACCOUNT_ACTIVATED_MANUALLY:"+locked.accountId(),locked.accountId(),"ACCOUNT_ACTIVATED_MANUALLY","ACCOUNT",locked.accountId(),Map.of("requestId",requestId.toString()));
		return getForActor(manager,requestId,AccountRole.MANAGER);}

	@Transactional
	ManualActivationResponse reject(UUID manager,UUID requestId,ManualActivationDecisionRequest request){ensure(manager,AccountRole.MANAGER);limits.check("MANUAL_DECISION_ACTOR",manager.toString(),30,Duration.ofMinutes(1));Instant now=clock.instant();RequestLock locked=lockRequest(requestId);
		if(locked.status()!=ManualActivationStatus.PENDING)throw error(HttpStatus.CONFLICT,"MANUAL_ACTIVATION_REQUEST_DECIDED","Manual activation request is no longer pending.");
		db.update("update email_verification_manual_activation_requests set status='REJECTED',reviewed_by_account_id=?,decision_note=?,reviewed_at=?,updated_at=?,version=version+1 where id=?",manager,request.decisionNote().trim(),Timestamp.from(now),Timestamp.from(now),requestId);
		audits.save(new AuditEvent(UUID.randomUUID(),manager,"MANUAL_ACTIVATION_REQUEST_REJECTED","MANUAL_ACTIVATION_REQUEST",requestId,
			Map.of("accountId",locked.accountId().toString(),"requestedByAccountId",locked.requestedBy().toString(),
				"reasonCode",locked.reasonCode(),"verificationMethod",locked.verificationMethod())));
		return getForActor(manager,requestId,AccountRole.MANAGER);}

	private RequestLock lockRequest(UUID id){List<RequestLock> rows=db.query("select account_id,requested_by_account_id,status,expires_at,reason_code,verification_method from email_verification_manual_activation_requests where id=? for update",(rs,n)->new RequestLock(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),ManualActivationStatus.valueOf(rs.getString(3)),rs.getTimestamp(4).toInstant(),rs.getString(5),rs.getString(6)),id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"MANUAL_ACTIVATION_REQUEST_NOT_FOUND","Manual activation request was not found.");return rows.getFirst();}
	private void terminateArtifacts(UUID account,Instant now){db.update("update email_verification_challenges set superseded_at=coalesce(superseded_at,?),updated_at=?,version=version+1 where account_id=? and consumed_at is null",Timestamp.from(now),Timestamp.from(now),account);db.update("update email_verification_sessions set revoked_at=coalesce(revoked_at,?),updated_at=? where account_id=?",Timestamp.from(now),Timestamp.from(now),account);db.update("update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,locked_by=null,updated_at=? where account_id=? and status in ('PENDING','FAILED','PROCESSING')",Timestamp.from(now),account);}
	private void ensure(UUID id,AccountRole role){if(!accounts.existsByIdAndRoleAndStatus(id,role,AccountStatus.ACTIVE))throw new InvalidAuthenticatedAccountException();}
	private RequesterSnapshot requesterSnapshot(UUID id){return db.queryForObject(
		"select full_name,role from accounts where id=?",(rs,n)->new RequesterSnapshot(rs.getString(1),AccountRole.valueOf(rs.getString(2))),id);}
	private void validatePage(int page,int size){if(page<0||size<1||size>100)throw error(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","page must be >= 0 and size between 1 and 100.");}
	private EmailVerificationException error(HttpStatus status,String code,String message){return new EmailVerificationException(status,code,message);}
	private String baseSelect(){return """
		select r.id,r.account_id,p.member_code,a.email,r.requested_by_account_id,r.requested_by_name_snapshot,
		 r.requested_by_role_snapshot,r.reason_code,r.reason_detail,
		 r.verification_method,r.evidence_note,r.status,r.reviewed_by_account_id,r.decision_note,r.requested_at,r.reviewed_at,r.expires_at
		from email_verification_manual_activation_requests r join accounts a on a.id=r.account_id join member_profiles p on p.account_id=a.id
		""";}
	private ManualActivationResponse map(ResultSet rs,int n)throws SQLException{return new ManualActivationResponse(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),EmailVerificationService.mask(rs.getString(4)),rs.getObject(5,UUID.class),rs.getString(6),AccountRole.valueOf(rs.getString(7)),ManualActivationReasonCode.valueOf(rs.getString(8)),rs.getString(9),ManualVerificationMethod.valueOf(rs.getString(10)),rs.getString(11),ManualActivationStatus.valueOf(rs.getString(12)),rs.getObject(13,UUID.class),rs.getString(14),rs.getTimestamp(15).toInstant(),rs.getTimestamp(16)==null?null:rs.getTimestamp(16).toInstant(),rs.getTimestamp(17).toInstant());}
	private record PendingAccount(UUID id,String memberCode,String email,Instant expiresAt){}
	private record RequesterSnapshot(String name,AccountRole role){}
	private record RequestLock(UUID accountId,UUID requestedBy,ManualActivationStatus status,Instant expiresAt,String reasonCode,String verificationMethod){}
}
