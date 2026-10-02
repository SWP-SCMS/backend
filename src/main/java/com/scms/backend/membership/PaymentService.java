package com.scms.backend.membership;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
 private final JdbcTemplate db; private final AccountRepository accounts; private final AuditEventRepository audits; private final Clock clock;
 PaymentService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, Clock clock){this.db=db;this.accounts=accounts;this.audits=audits;this.clock=clock;}
 @Transactional public Map<String,Object> cash(UUID actor, UUID order){ensure(actor,AccountRole.RECEPTIONIST); return settle(actor,order,"CASH",null,"cash");}
 @Transactional public Map<String,Object> bankTransfer(UUID actor, UUID order){
  if(!accounts.existsByIdAndRoleAndStatus(actor,AccountRole.MEMBER,AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException();
  var o=db.queryForMap("select * from membership_orders where id=? and member_account_id=?",order,actor);
  UUID payment=UUID.randomUUID(); db.update("insert into payments(id,order_id,method,status,amount,currency_code) values(?,?, 'BANK_TRANSFER','PENDING',?,'VND')",payment,order,o.get("price_amount_snapshot"));
  return Map.of("paymentId",payment,"orderId",order,"status","PENDING");
 }
 @Transactional(readOnly=true) public java.util.List<Map<String,Object>> receipts(UUID actor){
  ensureAny(actor); return db.queryForList("select id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot,issued_at from receipts where member_account_id=? order by issued_at desc",actor);
 }
 @Transactional(readOnly=true) public java.util.List<Map<String,Object>> memberHistory(UUID actor){
  ensure(actor,AccountRole.MEMBER); return db.queryForList("select id,order_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at from memberships where member_account_id=? order by starts_at desc",actor);
 }
 @Transactional(readOnly=true) public Map<String,Object> report(UUID actor){
  ensure(actor,AccountRole.MANAGER); return db.queryForMap("select count(*) filter (where status='PAID') paid_payments, coalesce(sum(amount) filter (where status='PAID'),0) paid_revenue, count(*) filter (where status='PENDING') pending_payments, count(*) filter (where status='FAILED') failed_payments from payments");
 }
 @Transactional public Map<String,Object> reconcile(UUID actor, UUID payment, PaymentActionRequest r){
  ensureAny(actor); var p=db.queryForMap("select * from payments where id=? for update",payment); String status=(String)p.get("status");
  if("PAID".equals(status)) return p; if(r==null||r.reason()==null||r.reason().isBlank()) throw new IllegalArgumentException("reason is required");
  String method=(String)p.get("method"); if("PAID".equalsIgnoreCase(r.status())) return settle(actor,(UUID)p.get("order_id"),method,r.providerReference(),r.reason());
  db.update("update payments set status='FAILED', failure_reason=?, updated_at=current_timestamp where id=? and status='PENDING'",r.reason(),payment);
  db.update("update membership_orders set status='EXPIRED', updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'",p.get("order_id"));
  audits.save(new AuditEvent(UUID.randomUUID(),actor,"PAYMENT_RECONCILED","PAYMENT",payment,r.reason(),Map.of("status",(Object)status),Map.of("status",(Object)"FAILED","evidence",(Object)String.valueOf(r.evidence())))); return db.queryForMap("select * from payments where id=?",payment);
 }
 private Map<String,Object> settle(UUID actor, UUID order, String method, String ref, String reason){
  var o=db.queryForMap("select * from membership_orders where id=? for update",order); if(!"PENDING_PAYMENT".equals(o.get("status"))&&db.queryForObject("select count(*) from payments where order_id=? and status='PAID'",Integer.class,order)==0) throw new IllegalArgumentException("Order is not payable");
  UUID payment=UUID.randomUUID(); Instant now=clock.instant(); db.update("insert into payments(id,order_id,method,status,amount,currency_code,provider_reference,processed_by_account_id,paid_at) values(?,?,?,'PAID',?,'VND',?,?,?)",payment,order,method,o.get("price_amount_snapshot"),ref,actor,now);
  db.update("update membership_orders set status='PAID',paid_at=?,updated_at=current_timestamp where id=?",now,order);
  UUID membership=UUID.randomUUID(); db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,?,?,?,?,'ACTIVE',?,?)",membership,o.get("member_account_id"),order,o.get("offer_id"),o.get("plan_code_snapshot"),o.get("offer_name_snapshot"),o.get("price_amount_snapshot"),o.get("currency_code_snapshot"),o.get("duration_days_snapshot"),now,now.plusSeconds(((Number)o.get("duration_days_snapshot")).longValue()*86400));
  UUID receipt=UUID.randomUUID(); db.update("insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot) values(?,?,?,?,?,?,?,?)",receipt,"RC-"+receipt, payment,order,o.get("member_account_id"),o.get("price_amount_snapshot"),o.get("currency_code_snapshot"),method);
  audits.save(new AuditEvent(UUID.randomUUID(),actor,"PAYMENT_PAID","PAYMENT",payment,reason,Map.of("status",(Object)"PENDING"),Map.of("status",(Object)"PAID"))); return Map.of("paymentId",payment,"membershipId",membership,"receiptId",receipt,"status","PAID");
 }
 private void ensure(UUID id,AccountRole role){if(!accounts.existsByIdAndRoleAndStatus(id,role,AccountStatus.ACTIVE))throw new InvalidAuthenticatedAccountException();}
 private void ensureAny(UUID id){if(!accounts.existsByIdAndRoleAndStatus(id,AccountRole.MANAGER,AccountStatus.ACTIVE)&&!accounts.existsByIdAndRoleAndStatus(id,AccountRole.RECEPTIONIST,AccountStatus.ACTIVE))throw new InvalidAuthenticatedAccountException();}
}
